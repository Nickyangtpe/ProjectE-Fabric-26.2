package moze_intel.projecte.mixin;

import moze_intel.projecte.compat.conditions.ConditionalResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
abstract class RecipeManagerConditionsMixin {
  @ModifyVariable(
      method =
          "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0)
  private ResourceManager projecte$evaluateRecipeConditions(ResourceManager resourceManager) {
    return new ConditionalResourceManager(resourceManager);
  }
}
