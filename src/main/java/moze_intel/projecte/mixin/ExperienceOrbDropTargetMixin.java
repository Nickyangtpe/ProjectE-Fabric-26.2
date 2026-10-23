package moze_intel.projecte.mixin;

import moze_intel.projecte.utils.ExperienceDropTarget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbDropTargetMixin {
  @ModifyVariable(
      method =
          "awardWithDirection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0)
  private static Vec3 projecte$useCollectedLootPosition(
      Vec3 position, ServerLevel level, Vec3 original, Vec3 direction, int amount) {
    // Redirect before native merging: existing orbs at the destroyed blocks are untouched.
    return ExperienceDropTarget.position(level, position);
  }
}
