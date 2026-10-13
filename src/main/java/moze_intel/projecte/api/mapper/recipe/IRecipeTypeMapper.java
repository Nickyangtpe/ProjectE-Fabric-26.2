package moze_intel.projecte.api.mapper.recipe;

import moze_intel.projecte.api.config.IConfigurableElement;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public interface IRecipeTypeMapper extends IConfigurableElement {
  @Override
  default boolean isAvailable() {
    return IConfigurableElement.super.isAvailable();
  }

  boolean canHandle(RecipeType<?> var1);

  boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> var1,
      RecipeHolder<?> var2,
      RegistryAccess var3,
      INSSFakeGroupManager var4);
}
