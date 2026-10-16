package moze_intel.projecte.gameObjs.container.inventory;

import java.util.Set;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.components.GemData;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.jetbrains.annotations.NotNull;

public class EternalDensityInventory extends ItemStackHandler {
  private final ItemStack invItem;
  private final boolean remote;

  public EternalDensityInventory(ItemStack stack, boolean remote) {
    super(9);
    this.invItem = stack;
    this.remote = remote;
    int slot = 0;

    for (ItemStack whitelisted :
        ((GemData) this.invItem.getOrDefault(PEDataComponentTypes.GEM_DATA.get(), GemData.EMPTY))
            .whitelist()) {
      if (!whitelisted.isEmpty()) {
        this.stacks.set(slot++, whitelisted.copy());
        if (slot >= this.getSlots()) {
          break;
        }
      }
    }
  }

  @Override
  public int getSlots() {
    return 9;
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    if (stack.isEmpty()) {
      return true;
    }

    int i = 0;

    for (int slots = this.getSlots(); i < slots; i++) {
      if (ItemStack.isSameItemSameComponents(stack, this.getStackInSlot(i))) {
        return i == slot;
      }
    }

    return true;
  }

  @Override
  public void setStackInSlot(int slot, @NotNull ItemStack stack) {
    if (this.isItemValid(slot, stack)) {
      super.setStackInSlot(slot, stack);
    }
  }

  @Override
  public int getSlotLimit(int slot) {
    return 1;
  }

  @Override
  protected void onContentsChanged(int slot) {
    if (!this.remote) {
      Set<ItemStack> targets = ItemStackLinkedSet.createTypeAndComponentsSet();
      int i = 0;

      for (int slots = this.getSlots(); i < slots; i++) {
        ItemStack stackInSlot = this.getStackInSlot(i);
        if (!stackInSlot.isEmpty()) {
          targets.add(stackInSlot.copyWithCount(1));
        }
      }

      this.invItem.update(
          PEDataComponentTypes.GEM_DATA.get(), GemData.EMPTY, targets, GemData::withWhitelistSafe);
    }
  }
}
