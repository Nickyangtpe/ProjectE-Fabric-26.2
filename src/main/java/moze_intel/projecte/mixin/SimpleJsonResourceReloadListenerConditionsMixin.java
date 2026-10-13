package moze_intel.projecte.mixin;

import moze_intel.projecte.compat.conditions.ConditionalResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SimpleJsonResourceReloadListener.class)
abstract class SimpleJsonResourceReloadListenerConditionsMixin {
  @ModifyVariable(
      method =
          "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0)
  private ResourceManager projecte$evaluateAdvancementConditions(ResourceManager resourceManager) {
    return new ConditionalResourceManager(resourceManager);
  }
}
