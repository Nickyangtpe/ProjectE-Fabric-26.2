package moze_intel.projecte.gameObjs.customRecipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.gameObjs.registries.PERecipeSerializers;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PhiloStoneSmeltingRecipe extends CustomRecipe {
  public boolean matches(@NotNull CraftingInput inv, @NotNull Level level) {
    return !this.getMatchingRecipes(inv, level).isEmpty();
  }

  @NotNull
  public ItemStack assemble(@NotNull CraftingInput inv) {
    Set<RecipeHolder<SmeltingRecipe>> matchingRecipes =
        this.getMatchingRecipes(inv, ServerLifecycleHooks.getCurrentServer().overworld());
    if (matchingRecipes.isEmpty()) {
      return ItemStack.EMPTY;
    }

    ItemStack output =
        ((SmeltingRecipe) matchingRecipes.stream().findFirst().get().value())
            .assemble(new SingleRecipeInput(findSmeltableInput(inv)));
    return output.copyWithCount(output.getCount() * 7);
  }

  private Set<RecipeHolder<SmeltingRecipe>> getMatchingRecipes(
      CraftingInput inv, @NotNull Level level) {
    List<ItemStack> philoStones = new ArrayList<>();
    List<ItemStack> coals = new ArrayList<>();
    List<ItemStack> allItems = new ArrayList<>();

    for (ItemStack stack : inv.items()) {
      if (!stack.isEmpty()) {
        allItems.add(stack);
        if (allItems.size() > 9) {
          return Collections.emptySet();
        }

        if (stack.is(PEItems.PHILOSOPHERS_STONE.get())) {
          philoStones.add(stack);
        }

        if (stack.is(ItemTags.COALS)) {
          coals.add(stack);
        }
      }
    }

    if (allItems.size() == 9) {
      for (ItemStack philoStone : philoStones) {
        for (ItemStack coal : coals) {
          if (philoStone != coal) {
            Set<RecipeHolder<SmeltingRecipe>> matchingRecipes = new HashSet<>();

            for (ItemStack stack : allItems) {
              if (stack != philoStone && stack != coal) {
                SingleRecipeInput furnaceInput = new SingleRecipeInput(stack);
                if (matchingRecipes.isEmpty()) {
                  if (!matchingRecipes.addAll(getMatchingSmeltingRecipes(level, furnaceInput))) {
                    return Collections.emptySet();
                  }
                } else {
                  Iterator<RecipeHolder<SmeltingRecipe>> iterator = matchingRecipes.iterator();

                  while (iterator.hasNext()) {
                    RecipeHolder<SmeltingRecipe> recipe = iterator.next();
                    if (!((SmeltingRecipe) recipe.value()).matches(furnaceInput, level)) {
                      iterator.remove();
                    }
                  }

                  if (matchingRecipes.isEmpty()) {
                    return Collections.emptySet();
                  }
                }
              }
            }

            if (!matchingRecipes.isEmpty()) {
              return matchingRecipes;
            }
          }
        }
      }
    }

    return Collections.emptySet();
  }

  @NotNull
  public RecipeSerializer<? extends PhiloStoneSmeltingRecipe> getSerializer() {
    return PERecipeSerializers.PHILO_STONE_SMELTING.get();
  }

  private static ItemStack findSmeltableInput(CraftingInput inv) {
    for (ItemStack stack : inv.items()) {
      if (!stack.isEmpty()
          && !stack.is(PEItems.PHILOSOPHERS_STONE.get())
          && !stack.is(ItemTags.COALS)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  private static Set<RecipeHolder<SmeltingRecipe>> getMatchingSmeltingRecipes(
      Level level, SingleRecipeInput input) {
    if (level.recipeAccess() instanceof RecipeManager recipeManager) {
      HashSet var8 = new HashSet();

      for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
        if (holder.value() instanceof SmeltingRecipe recipe && recipe.matches(input, level)) {
          var8.add(holder);
        }
      }

      return var8;
    } else {
      return Collections.emptySet();
    }
  }
}
