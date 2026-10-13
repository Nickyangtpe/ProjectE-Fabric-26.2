package moze_intel.projecte.gameObjs.container.slots;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;

public class InventoryContainerSlot extends SlotItemHandler implements IInventoryContainerSlot {
  public InventoryContainerSlot(IItemHandler itemHandler, int index, int x, int y) {
    super(itemHandler, index, x, y);
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
  }
}
