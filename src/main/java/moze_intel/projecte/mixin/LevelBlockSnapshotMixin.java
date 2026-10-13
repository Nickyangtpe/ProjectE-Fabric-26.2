package moze_intel.projecte.mixin;

import moze_intel.projecte.compat.util.BlockSnapshotCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelBlockSnapshotMixin {
  @Inject(
      method =
          "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
      at = @At("HEAD"))
  private void projecte$captureBeforeMutation(
      BlockPos pos,
      BlockState state,
      int flags,
      int recursionLeft,
      CallbackInfoReturnable<Boolean> cir) {
    BlockSnapshotCapture.captureBeforeMutation((Level) (Object) this, pos, flags);
  }
}
