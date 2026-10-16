package moze_intel.projecte.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import moze_intel.projecte.events.PlayerRender;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
abstract class CameraFovMixin {
  @ModifyExpressionValue(
      method = "tickFov",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/player/AbstractClientPlayer;getFieldOfViewModifier(ZF)F"))
  private float projecte$applyGemBootsFovModifier(float vanillaModifier) {
    return PlayerRender.applyGemBootsFovModifier(vanillaModifier);
  }
}
