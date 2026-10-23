package moze_intel.projecte.compat.items;

import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackHandler implements IItemHandlerModifiable {
  protected NonNullList<ItemStack> stacks;

  public ItemStackHandler() {
    this(1);
  }

  public ItemStackHandler(int size) {
    this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
  }

  public ItemStackHandler(NonNullList<ItemStack> stacks) {
    this.stacks = stacks;
  }

  public void setSize(int size) {
    this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
  }

  @Override
  public void setStackInSlot(int slot, @NotNull ItemStack stack) {
    this.validateSlotIndex(slot);
    this.stacks.set(slot, stack);
    this.onContentsChanged(slot);
  }

  @Override
  public int getSlots() {
    return this.stacks.size();
  }

  @NotNull
  @Override
  public ItemStack getStackInSlot(int slot) {
    this.validateSlotIndex(slot);
    return (ItemStack) this.stacks.get(slot);
  }

  @NotNull
  @Override
  public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    this.validateSlotIndex(slot);
    if (!this.isItemValid(slot, stack)) {
      return stack;
    }

    ItemStack existing = (ItemStack) this.stacks.get(slot);
    int limit = this.getStackLimit(slot, stack);
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
      if (existing.isEmpty()) {
        this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack.copy());
      } else {
        // The free capacity is not the number of items supplied by the caller.
        existing.grow(reachedLimit ? limit : stack.getCount());
      }

      this.onContentsChanged(slot);
    }

    return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
  }

  @NotNull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    if (amount <= 0) {
      return ItemStack.EMPTY;
    }

    this.validateSlotIndex(slot);
    ItemStack existing = (ItemStack) this.stacks.get(slot);
    if (existing.isEmpty()) {
      return ItemStack.EMPTY;
    }

    int toExtract = Math.min(amount, existing.getMaxStackSize());
    if (existing.getCount() <= toExtract) {
      if (!simulate) {
        this.stacks.set(slot, ItemStack.EMPTY);
        this.onContentsChanged(slot);
        return existing;
      } else {
        return existing.copy();
      }
    } else {
      if (!simulate) {
        this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
        this.onContentsChanged(slot);
      }

      return existing.copyWithCount(toExtract);
    }
  }

  @Override
  public int getSlotLimit(int slot) {
    return 64;
  }

  protected int getStackLimit(int slot, @NotNull ItemStack stack) {
    return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    return true;
  }

  @NotNull
  public CompoundTag serializeNBT(Provider provider) {
    ListTag nbtTagList = new ListTag();

    for (int i = 0; i < this.stacks.size(); i++) {
      if (!((ItemStack) this.stacks.get(i)).isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putInt("Slot", i);
        ItemStack.CODEC
            .encodeStart(
                provider.createSerializationContext(NbtOps.INSTANCE),
                (ItemStack) this.stacks.get(i))
            .result()
            .ifPresent(tag -> itemTag.put("Item", tag));
        nbtTagList.add(itemTag);
      }
    }

    CompoundTag nbt = new CompoundTag();
    nbt.put("Items", nbtTagList);
    nbt.putInt("Size", this.stacks.size());
    return nbt;
  }

  public void deserializeNBT(Provider provider, CompoundTag nbt) {
    this.setSize(nbt.getIntOr("Size", this.stacks.size()));
    ListTag tagList = nbt.getListOrEmpty("Items");

    for (int i = 0; i < tagList.size(); i++) {
      CompoundTag itemTags = tagList.getCompoundOrEmpty(i);
      int slot = itemTags.getIntOr("Slot", -1);
      if (slot >= 0 && slot < this.stacks.size()) {
        int targetSlot = slot;
        itemTags
            .getCompound("Item")
            .ifPresent(
                tag ->
                    ItemStack.CODEC
                        .parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                        .result()
                        .ifPresent(stack -> this.stacks.set(targetSlot, stack)));
      }
    }

    this.onLoad();
  }

  protected void onContentsChanged(int slot) {}

  protected void onLoad() {}

  protected void validateSlotIndex(int slot) {
    if (slot < 0 || slot >= this.stacks.size()) {
      throw new RuntimeException(
          "Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
    }
  }
}
