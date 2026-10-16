package moze_intel.projecte.emc.mappers.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

@RecipeTypeMapper(priority = Integer.MIN_VALUE)
public class FallbackRecipeTypeMapper extends BaseRecipeTypeMapper {
  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_FALLBACK.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_FALLBACK.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_FALLBACK.tooltip();
  }

  @Override
  public boolean canHandle(RecipeType<?> recipeType) {
    return true;
  }

  @Override
  public boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RecipeHolder<?> recipeHolder,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    Recipe<?> recipe = recipeHolder.value();
    return !(recipe instanceof CraftingRecipe)
            && !(recipe instanceof AbstractCookingRecipe)
            && !(recipe instanceof SingleItemRecipe)
            && !(recipe instanceof SmithingTransformRecipe)
            && !(recipe instanceof SmithingTrimRecipe)
        ? false
        : super.handleRecipe(mapper, recipeHolder, registryAccess, fakeGroupManager);
  }

  @Override
  protected Collection<Ingredient> getIngredients(Recipe<?> recipe) {
    Collection<Ingredient> ingredients = super.getIngredients(recipe);
    if (ingredients.isEmpty()) {
      if (recipe instanceof SmithingTransformRecipe transformRecipe) {
        List<Ingredient> smithingIngredients = new ArrayList<>(3);
        smithingIngredients.add(transformRecipe.baseIngredient());
        transformRecipe.additionIngredient().ifPresent(smithingIngredients::add);
        transformRecipe.templateIngredient().ifPresent(smithingIngredients::add);
        return smithingIngredients;
      }

      if (recipe instanceof SmithingTrimRecipe trimRecipe) {
        List<Ingredient> smithingIngredients = new ArrayList<>(3);
        smithingIngredients.add(trimRecipe.baseIngredient());
        trimRecipe.additionIngredient().ifPresent(smithingIngredients::add);
        trimRecipe.templateIngredient().ifPresent(smithingIngredients::add);
        return smithingIngredients;
      }
    }

    return ingredients;
  }
}
