package moze_intel.projecte.compat.items;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotItemHandler extends Slot {
  private final IItemHandler itemHandler;
  private final int handlerSlot;

  public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
    super(new SimpleContainer(0), index, xPosition, yPosition);
    this.itemHandler = itemHandler;
    this.handlerSlot = index;
  }

  public IItemHandler getItemHandler() {
    return this.itemHandler;
  }

  public boolean mayPlace(@NotNull ItemStack stack) {
    return stack.isEmpty() ? false : this.itemHandler.isItemValid(this.handlerSlot, stack);
  }

  @NotNull
  public ItemStack getItem() {
    return this.itemHandler.getStackInSlot(this.handlerSlot);
  }

  public boolean hasItem() {
    return !this.getItem().isEmpty();
  }

  public void set(@NotNull ItemStack stack) {
    ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(this.handlerSlot, stack);
    this.setChanged();
  }

  public void initialize(@NotNull ItemStack stack) {
    this.set(stack);
  }

  public void setChanged() {}

  @NotNull
  public ItemStack remove(int amount) {
    return this.itemHandler.extractItem(this.handlerSlot, amount, false);
  }

  public int getMaxStackSize() {
    return this.itemHandler.getSlotLimit(this.handlerSlot);
  }

  public int getMaxStackSize(@NotNull ItemStack stack) {
    return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
  }

  public boolean mayPickup(@NotNull Player player) {
    return true;
  }

  public int getContainerSlot() {
    return this.handlerSlot;
  }
}
