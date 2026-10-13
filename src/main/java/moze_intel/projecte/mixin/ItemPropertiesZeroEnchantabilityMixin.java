package moze_intel.projecte.mixin;

import net.minecraft.world.item.Item.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Properties.class)
public abstract class ItemPropertiesZeroEnchantabilityMixin {
  @Inject(method = "enchantable", at = @At("HEAD"), cancellable = true)
  private void projecte$preserveZeroEnchantability(
      int enchantability, CallbackInfoReturnable<Properties> cir) {
    if (enchantability == 0) {
      cir.setReturnValue((Properties) (Object) this);
    }
  }
}
