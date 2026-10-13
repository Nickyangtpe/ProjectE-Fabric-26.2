package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WrappedItemHandler implements IItemHandlerModifiable {
  private final IItemHandlerModifiable compose;
  private final WrappedItemHandler.WriteMode mode;

  public WrappedItemHandler(IItemHandlerModifiable compose, WrappedItemHandler.WriteMode mode) {
    this.compose = compose;
    this.mode = mode;
  }

  @Override
  public int getSlots() {
    return this.compose.getSlots();
  }

  @NotNull
  @Override
  public ItemStack getStackInSlot(int slot) {
    return this.compose.getStackInSlot(slot);
  }

  @NotNull
  @Override
  public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
    return this.mode != WrappedItemHandler.WriteMode.IN
            && this.mode != WrappedItemHandler.WriteMode.IN_OUT
        ? stack
        : this.compose.insertItem(slot, stack, simulate);
  }

  @NotNull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    return this.mode != WrappedItemHandler.WriteMode.OUT
            && this.mode != WrappedItemHandler.WriteMode.IN_OUT
        ? ItemStack.EMPTY
        : this.compose.extractItem(slot, amount, simulate);
  }

  @Override
  public int getSlotLimit(int slot) {
    return this.compose.getSlotLimit(slot);
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    return this.compose.isItemValid(slot, stack);
  }

  @Override
  public void setStackInSlot(int slot, @NotNull ItemStack stack) {
    this.compose.setStackInSlot(slot, stack);
  }

  public enum WriteMode {
    IN,
    OUT,
    IN_OUT,
    NONE;
  }
}
