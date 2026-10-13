package moze_intel.projecte.gameObjs.container.slots;

import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IInsertableSlot {
  private Slot self() {
    return (Slot) this;
  }

  @NotNull
  default ItemStack insertItem(@NotNull ItemStack stack, boolean simulate) {
    Slot self = this.self();
    if (!stack.isEmpty() && self.mayPlace(stack)) {
      ItemStack current = self.getItem();
      int needed = self.getMaxStackSize(stack) - current.getCount();
      if (needed <= 0) {
        return stack;
      }

      if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
        return stack;
      }

      int toAdd = Math.min(stack.getCount(), needed);
      if (!simulate) {
        self.set(stack.copyWithCount(current.getCount() + toAdd));
      }

      return ItemHelper.size(stack, stack.getCount() - toAdd);
    } else {
      return stack;
    }
  }
}
