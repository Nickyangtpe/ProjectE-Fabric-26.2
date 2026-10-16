package moze_intel.projecte.emc.mappers.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

@RecipeTypeMapper
public class SmithingRecipeMapper extends BaseRecipeTypeMapper {
  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SMITHING.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SMITHING.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SMITHING.tooltip();
  }

  @Override
  public boolean canHandle(RecipeType<?> recipeType) {
    return recipeType == RecipeType.SMITHING;
  }

  @Override
  public boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RecipeHolder<?> recipeHolder,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    return recipeHolder.value() instanceof SmithingTrimRecipe
        ? true
        : super.handleRecipe(mapper, recipeHolder, registryAccess, fakeGroupManager);
  }

  @Override
  protected Collection<Ingredient> getIngredients(Recipe<?> recipe) {
    if (recipe instanceof SmithingTransformRecipe transformRecipe) {
      List<Ingredient> ingredients = new ArrayList<>(3);
      ingredients.add(transformRecipe.baseIngredient());
      transformRecipe.additionIngredient().ifPresent(ingredients::add);
      transformRecipe.templateIngredient().ifPresent(ingredients::add);
      return ingredients;
    } else if (recipe instanceof SmithingTrimRecipe trimRecipe) {
      List<Ingredient> ingredients = new ArrayList<>(3);
      ingredients.add(trimRecipe.baseIngredient());
      trimRecipe.additionIngredient().ifPresent(ingredients::add);
      trimRecipe.templateIngredient().ifPresent(ingredients::add);
      return ingredients;
    } else {
      return Collections.emptyList();
    }
  }
}
