package moze_intel.projecte.compat.items.wrapper;

import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerMainInvWrapper implements IItemHandlerModifiable {
  private final Inventory inventory;

  public PlayerMainInvWrapper(Inventory inventory) {
    this.inventory = inventory;
  }

  private int mainSlots() {
    return this.inventory.getNonEquipmentItems().size();
  }

  @Override
  public int getSlots() {
    return this.mainSlots();
  }

  @NotNull
  @Override
  public ItemStack getStackInSlot(int slot) {
    this.validateSlotIndex(slot);
    return this.inventory.getItem(slot);
  }

  @Override
  public void setStackInSlot(int slot, @NotNull ItemStack stack) {
    this.validateSlotIndex(slot);
    this.inventory.setItem(slot, stack);
    this.inventory.setChanged();
  }

  @NotNull
  @Override
  public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    this.validateSlotIndex(slot);
    ItemStack existing = this.inventory.getItem(slot);
    int limit = Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
    if (!existing.isEmpty()) {
      if (!ItemStack.isSameItemSameComponents(stack, existing)) {
        return stack;
      }

      limit -= existing.getCount();
    }

    if (limit <= 0) {
      return stack;
    }

    int accepted = Math.min(stack.getCount(), limit);
    if (!simulate) {
      if (existing.isEmpty()) {
        this.inventory.setItem(slot, stack.copyWithCount(accepted));
      } else {
        existing.grow(accepted);
      }

      this.inventory.setChanged();
    }

    return accepted == stack.getCount()
        ? ItemStack.EMPTY
        : stack.copyWithCount(stack.getCount() - accepted);
  }

  @NotNull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    if (amount <= 0) {
      return ItemStack.EMPTY;
    }

    this.validateSlotIndex(slot);
    ItemStack existing = this.inventory.getItem(slot);
    if (existing.isEmpty()) {
      return ItemStack.EMPTY;
    }

    int toExtract = Math.min(amount, existing.getMaxStackSize());
    if (existing.getCount() <= toExtract) {
      if (!simulate) {
        this.inventory.setItem(slot, ItemStack.EMPTY);
        this.inventory.setChanged();
        return existing;
      } else {
        return existing.copy();
      }
    } else {
      if (!simulate) {
        this.inventory.setItem(slot, existing.copyWithCount(existing.getCount() - toExtract));
        this.inventory.setChanged();
      }

      return existing.copyWithCount(toExtract);
    }
  }

  @Override
  public int getSlotLimit(int slot) {
    return 64;
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    return true;
  }

  private void validateSlotIndex(int slot) {
    if (slot < 0 || slot >= this.mainSlots()) {
      throw new RuntimeException(
          "Slot " + slot + " not in valid range - [0," + this.mainSlots() + ")");
    }
  }
}
