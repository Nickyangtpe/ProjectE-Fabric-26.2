package moze_intel.projecte.compat.items.wrapper;

import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RangedWrapper implements IItemHandlerModifiable {
  private final IItemHandlerModifiable compose;
  private final int minSlot;
  private final int maxSlot;

  public RangedWrapper(IItemHandlerModifiable compose, int minSlot, int maxSlotExclusive) {
    this.compose = compose;
    this.minSlot = minSlot;
    this.maxSlot = maxSlotExclusive;
  }

  @Override
  public int getSlots() {
    return this.maxSlot - this.minSlot;
  }

  @NotNull
  @Override
  public ItemStack getStackInSlot(int slot) {
    return this.compose.getStackInSlot(this.mapSlot(slot));
  }

  @Override
  public void setStackInSlot(int slot, @NotNull ItemStack stack) {
    this.compose.setStackInSlot(this.mapSlot(slot), stack);
  }

  @NotNull
  @Override
  public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
    return this.compose.insertItem(this.mapSlot(slot), stack, simulate);
  }

  @NotNull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    return this.compose.extractItem(this.mapSlot(slot), amount, simulate);
  }

  @Override
  public int getSlotLimit(int slot) {
    return this.compose.getSlotLimit(this.mapSlot(slot));
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    return this.compose.isItemValid(this.mapSlot(slot), stack);
  }

  private int mapSlot(int slot) {
    int mapped = slot + this.minSlot;
    if (mapped >= this.minSlot && mapped < this.maxSlot) {
      return mapped;
    } else {
      throw new RuntimeException(
          "Slot " + slot + " not in valid range - [0," + this.getSlots() + ")");
    }
  }
}
