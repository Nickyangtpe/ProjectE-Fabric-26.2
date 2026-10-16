package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.blocks.ProjectETNT;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin {
  @Inject(
      method =
          "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z",
      at = @At("HEAD"),
      cancellable = true)
  private static void projecte$primeCustomTnt(
      Level level,
      BlockPos pos,
      @Nullable LivingEntity igniter,
      CallbackInfoReturnable<Boolean> cir) {
    if (level instanceof ServerLevel serverLevel) {
      if (!(Boolean) serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
        cir.setReturnValue(false);
      } else {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ProjectETNT tnt) {
          tnt.onCaughtFire(state, level, pos, null, igniter);
          cir.setReturnValue(true);
        }
      }
    }
  }
}
