package moze_intel.projecte.compat.items;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.NotNull;

public class ComponentItemHandler implements IItemHandlerModifiable {
  protected final ItemStack parent;
  protected final DataComponentType<ItemContainerContents> component;
  private final int size;

  public ComponentItemHandler(
      ItemStack parent, DataComponentType<ItemContainerContents> component, int size) {
    this.parent = parent;
    this.component = component;
    this.size = size;
  }

  protected ItemContainerContents getComponentContents() {
    ItemContainerContents contents = (ItemContainerContents) this.parent.get(this.component);
    return contents == null ? ItemContainerContents.EMPTY : contents;
  }

  protected NonNullList<ItemStack> getContents() {
    NonNullList<ItemStack> stacks = NonNullList.withSize(this.size, ItemStack.EMPTY);
    this.getComponentContents().copyInto(stacks);
    return stacks;
  }

  protected void updateContents(
      @NotNull ItemContainerContents contents, @NotNull ItemStack stack, int slot) {
    NonNullList<ItemStack> list = NonNullList.withSize(this.size, ItemStack.EMPTY);
    contents.copyInto(list);
    list.set(slot, stack);
    this.parent.set(this.component, ItemContainerContents.fromItems(list));
  }

  @Override
  public void setStackInSlot(int slot, @NotNull ItemStack stack) {
    this.validateSlotIndex(slot);
    this.updateContents(this.getComponentContents(), stack, slot);
  }

  @Override
  public int getSlots() {
    return this.size;
  }

  @NotNull
  @Override
  public ItemStack getStackInSlot(int slot) {
    this.validateSlotIndex(slot);
    return (ItemStack) this.getContents().get(slot);
  }

  @NotNull
  @Override
  public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    this.validateSlotIndex(slot);
    ItemStack existing = this.getStackInSlot(slot);
    int limit = Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
    if (!existing.isEmpty()) {
      if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) {
        return stack;
      }

      limit -= existing.getCount();
    }

    if (limit <= 0) {
      return stack;
    }

    boolean reachedLimit = stack.getCount() > limit;
    if (!simulate) {
      int newCount = existing.getCount() + (reachedLimit ? limit : stack.getCount());
      ItemStack result =
          existing.isEmpty() ? stack.copyWithCount(newCount) : existing.copyWithCount(newCount);
      this.setStackInSlot(slot, result);
    }

    return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
  }

  @NotNull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    if (amount == 0) {
      return ItemStack.EMPTY;
    }

    this.validateSlotIndex(slot);
    ItemStack existing = this.getStackInSlot(slot);
    if (existing.isEmpty()) {
      return ItemStack.EMPTY;
    }

    int toExtract = Math.min(amount, existing.getMaxStackSize());
    if (existing.getCount() <= toExtract) {
      if (!simulate) {
        this.setStackInSlot(slot, ItemStack.EMPTY);
        return existing;
      } else {
        return existing.copy();
      }
    } else {
      if (!simulate) {
        this.setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
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

  protected void validateSlotIndex(int slot) {
    if (slot < 0 || slot >= this.size) {
      throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.size + ")");
    }
  }
}
