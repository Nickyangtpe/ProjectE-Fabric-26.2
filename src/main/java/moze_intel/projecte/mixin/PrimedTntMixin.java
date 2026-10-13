package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.entity.EntityNovaPrimed;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {
  @Inject(method = "explode()V", at = @At("HEAD"), cancellable = true)
  private void projecte$explodeNova(CallbackInfo ci) {
    if ((Object) this instanceof EntityNovaPrimed nova) {
      if (nova.level() instanceof ServerLevel level
          && (Boolean) level.getGameRules().get(GameRules.TNT_EXPLODES)) {
        nova.explodeProjectE();
      }

      ci.cancel();
    }
  }
}
