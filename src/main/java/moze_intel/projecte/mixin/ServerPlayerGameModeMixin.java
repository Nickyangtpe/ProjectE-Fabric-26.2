package moze_intel.projecte.mixin;

import moze_intel.projecte.compat.event.ToolModeBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
  @Shadow @Final private ServerPlayer player;

  @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
  private void projecte$captureBlockBreakAction(
      BlockPos pos,
      Action action,
      Direction direction,
      int maxBuildHeight,
      int sequence,
      CallbackInfo ci) {
    ToolModeBreakEvents.captureDigAction(this.player, pos, action, direction);
  }
}
