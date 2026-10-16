package moze_intel.projecte.emc.mappers.recipe.special;

import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public abstract class DisplayBasedCraftingMapper implements IRecipeTypeMapper {
  protected abstract boolean matchesRecipe(RecipeHolder<?> var1);

  protected abstract boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> var1,
      RegistryAccess var2,
      INSSFakeGroupManager var3);

  @Override
  public final boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RecipeHolder<?> recipeHolder,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    return this.matchesRecipe(recipeHolder)
        && this.handleRecipe(mapper, registryAccess, fakeGroupManager);
  }

  @Override
  public final boolean canHandle(RecipeType<?> recipeType) {
    return recipeType == RecipeType.CRAFTING;
  }
}
