package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.items.ISelfCraftingRemainder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingRecipe.class)
public interface CraftingRemainderMixin {
  @Inject(method = "defaultCraftingReminder", at = @At("RETURN"))
  private static void projecte$preserveReusableInputs(
      CraftingInput input, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
    NonNullList<ItemStack> remainders = (NonNullList<ItemStack>) cir.getReturnValue();

    for (int slot = 0; slot < input.size(); slot++) {
      ItemStack stack = input.getItem(slot);
      if (!stack.isEmpty() && stack.getItem() instanceof ISelfCraftingRemainder) {
        remainders.set(slot, stack.copy());
      }
    }
  }
}
