package moze_intel.projecte.mixin;

import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.entity.living.LivingEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityJumpMixin {
  @Inject(method = "jumpFromGround", at = @At("TAIL"))
  private void projecte$afterGroundJump(CallbackInfo callbackInfo) {
    NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent((LivingEntity) (Object) this));
  }
}
