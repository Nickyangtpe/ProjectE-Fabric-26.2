package moze_intel.projecte.compat.items;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemHandlerHelper {
  private ItemHandlerHelper() {}

  public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
    if (dest != null && !stack.isEmpty()) {
      for (int i = 0; i < dest.getSlots(); i++) {
        stack = dest.insertItem(i, stack, simulate);
        if (stack.isEmpty()) {
          return ItemStack.EMPTY;
        }
      }

      return stack;
    } else {
      return stack;
    }
  }

  public static ItemStack insertItemStacked(
      IItemHandler inventory, ItemStack stack, boolean simulate) {
    if (inventory == null || stack.isEmpty()) {
      return stack;
    }

    if (!stack.isStackable()) {
      return insertItem(inventory, stack, simulate);
    }

    int sizeInventory = inventory.getSlots();

    for (int i = 0; i < sizeInventory; i++) {
      ItemStack slot = inventory.getStackInSlot(i);
      if (ItemStack.isSameItemSameComponents(slot, stack)) {
        stack = inventory.insertItem(i, stack, simulate);
        if (stack.isEmpty()) {
          break;
        }
      }
    }

    if (!stack.isEmpty()) {
      for (int i = 0; i < sizeInventory; i++) {
        if (inventory.getStackInSlot(i).isEmpty()) {
          stack = inventory.insertItem(i, stack, simulate);
          if (stack.isEmpty()) {
            break;
          }
        }
      }
    }

    return stack;
  }

  public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
    return !a.isEmpty()
        && ItemStack.isSameItem(a, b)
        && a.getComponents().equals(b.getComponents());
  }

  public static boolean canItemStacksStackRelaxed(ItemStack a, ItemStack b) {
    return !a.isEmpty()
        && !b.isEmpty()
        && ItemStack.isSameItem(a, b)
        && b.isStackable()
        && a.isStackable() == b.isStackable();
  }

  public static int calcRedstoneFromInventory(@Nullable IItemHandler inv) {
    if (inv == null) {
      return 0;
    }

    int items = 0;
    float fillRatio = 0.0F;

    for (int i = 0; i < inv.getSlots(); i++) {
      ItemStack stack = inv.getStackInSlot(i);
      if (!stack.isEmpty()) {
        fillRatio +=
            (float) stack.getCount() / Math.min(inv.getSlotLimit(i), stack.getMaxStackSize());
        items++;
      }
    }

    if (items > 0) {
      fillRatio /= items;
    }

    return Mth.floor(fillRatio * 14.0F) + (items > 0 ? 1 : 0);
  }
}
