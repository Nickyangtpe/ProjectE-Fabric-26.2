package moze_intel.projecte.emc.mappers.recipe;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.gameObjs.items.ISelfCraftingRemainder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.util.context.ContextMap.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jetbrains.annotations.Nullable;

public abstract class BaseRecipeTypeMapper implements IRecipeTypeMapper {
  @Override
  public boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RecipeHolder<?> recipeHolder,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    Recipe<?> recipe = recipeHolder.value();
    if (recipe.isSpecial()) {
      return false;
    }

    ContextMap displayContext =
        new Builder()
            .withParameter(SlotDisplayContext.REGISTRIES, registryAccess)
            .create(SlotDisplayContext.CONTEXT);
    List<ItemStack> recipeOutputs =
        recipe.display().stream()
            .flatMap(display -> display.result().resolveForStacks(displayContext).stream())
            .filter(output -> !output.isEmpty())
            .toList();
    if (recipeOutputs.isEmpty()) {
      return false;
    }

    Collection<Ingredient> ingredientsChecked = this.getIngredientsChecked(recipeHolder);
    if (ingredientsChecked == null) {
      return true;
    }

    ResourceKey<Recipe<?>> recipeID = recipeHolder.id();
    Object2IntMap<NormalizedSimpleStack> ingredientMap = new Object2IntOpenHashMap<>();

    for (Ingredient recipeItem : ingredientsChecked) {
      if (!recipeItem.isEmpty()) {
        ItemStack[] matches = this.getMatchingStacks(recipeItem, recipeID);
        if (matches.length == 0) {
          return true;
        }

        if (matches.length == 1) {
          ItemStack match = matches[0];
          if (match.isEmpty()) {
            return false;
          }

          if (representsEmptyTag(match)) {
            return true;
          }

          if (this.addIngredient(ingredientMap, match, recipeID)) {
            return true;
          }
        } else {
          Object2IntMap<NormalizedSimpleStack> rawNSSMatches =
              new Object2IntOpenHashMap<NormalizedSimpleStack>(matches.length);
          List<ItemStack> stacks = new ArrayList<>(matches.length);

          for (ItemStack match : matches) {
            if (!match.isEmpty() && !representsEmptyTag(match)) {
              rawNSSMatches.put(NSSItem.createItem(match), 1);
              stacks.add(match);
            }
          }

          int count = stacks.size();
          if (count == 0) {
            return true;
          }

          if (count <= 1) {
            if (this.addIngredient(ingredientMap, stacks.getFirst(), recipeID)) {
              return true;
            }
          } else {
            INSSFakeGroupManager.FakeGroupData group =
                fakeGroupManager.getOrCreateFakeGroupDirect(rawNSSMatches, true, true);
            NormalizedSimpleStack dummy = group.dummy();
            ingredientMap.mergeInt(dummy, 1, Constants.INT_SUM);
            if (group.created()) {
              boolean success = false;

              for (ItemStack stack : stacks) {
                Object2IntMap<NormalizedSimpleStack> groupIngredientMap = new Object2IntArrayMap(2);
                if (!this.addIngredient(groupIngredientMap, stack, recipeID)) {
                  mapper.addConversion(1, dummy, groupIngredientMap);
                  success = true;
                }
              }

              if (!success) {
                return true;
              }
            }
          }
        }
      }
    }

    for (ItemStack recipeOutput : recipeOutputs) {
      mapper.addConversion(
          recipeOutput.getCount(), NSSItem.createItem(recipeOutput), ingredientMap);
    }

    return true;
  }

  private static boolean representsEmptyTag(ItemStack stack) {
    return stack.getItem() == Items.BARRIER
        && stack.getHoverName() instanceof MutableComponent hoverName
        && hoverName.getString().startsWith("Empty Tag: ");
  }

  private ItemStack[] getMatchingStacks(Ingredient ingredient, ResourceKey<Recipe<?>> recipeID) {
    try {
      return ingredient
          .items()
          .map(holder -> ((Item) holder.value()).getDefaultInstance())
          .toArray(ItemStack[]::new);
    } catch (Exception e) {
      CustomIngredient customIngredient =
          ingredient instanceof FabricIngredient fabricIngredient
              ? fabricIngredient.getCustomIngredient()
              : null;
      if (customIngredient != null) {
        Identifier name = customIngredient.getSerializer().getIdentifier();
        PECore.LOGGER.error(
            LogUtils.FATAL_MARKER,
            "Error mapping recipe {}. Ingredient of type: {} crashed when getting the matching"
                + " stacks. Please report this to {}.",
            new Object[] {recipeID.identifier(), name, name.getNamespace(), e});
      } else {
        PECore.LOGGER.error(
            LogUtils.FATAL_MARKER,
            "Error mapping recipe {}. Crashed when getting the matching stacks.",
            recipeID.identifier(),
            e);
      }

      return new ItemStack[0];
    }
  }

  private boolean addIngredient(
      Object2IntMap<NormalizedSimpleStack> ingredientMap,
      ItemStack stack,
      ResourceKey<Recipe<?>> recipeID) {
    stack = stack.copy();
    Item item = stack.getItem();
    boolean hasContainerItem = false;

    try {
      ItemStackTemplate remainderTemplate = item.getCraftingRemainder();
      ItemStack remainder =
          item instanceof ISelfCraftingRemainder
              ? stack.copy()
              : remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create();
      hasContainerItem = !remainder.isEmpty();
      if (hasContainerItem) {
        ingredientMap.mergeInt(NSSItem.createItem(remainder), -1, Constants.INT_SUM);
      }
    } catch (Exception e) {
      Identifier itemName = BuiltInRegistries.ITEM.getKey(item);
      if (hasContainerItem) {
        PECore.LOGGER.error(
            LogUtils.FATAL_MARKER,
            "Error mapping recipe {}. Item: {} reported that it has a container item, but errors"
                + " when trying to get the container item based on the stack in the recipe. Please"
                + " report this to {}.",
            new Object[] {recipeID, itemName, itemName.getNamespace(), e});
      } else {
        PECore.LOGGER.error(
            LogUtils.FATAL_MARKER,
            "Error mapping recipe {}. Item: {} crashed when checking if the stack in the recipe has"
                + " a container item. Please report this to {}.",
            new Object[] {recipeID.identifier(), itemName, itemName.getNamespace(), e});
      }

      return true;
    }

    ingredientMap.mergeInt(NSSItem.createItem(stack), 1, Constants.INT_SUM);
    return false;
  }

  @Nullable
  private Collection<Ingredient> getIngredientsChecked(RecipeHolder<?> recipeHolder) {
    try {
      return this.getIngredients(recipeHolder.value());
    } catch (Exception e) {
      ResourceKey<Recipe<?>> recipeID = recipeHolder.id();
      PECore.LOGGER.error(
          LogUtils.FATAL_MARKER,
          "Error mapping recipe {}. Failed to get ingredients. Please report this to {}.",
          new Object[] {recipeID, recipeID.identifier().getNamespace(), e});
      return null;
    }
  }

  protected Collection<Ingredient> getIngredients(Recipe<?> recipe) {
    return recipe.placementInfo().ingredients();
  }
}
