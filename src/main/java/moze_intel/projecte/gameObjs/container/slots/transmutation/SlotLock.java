package moze_intel.projecte.gameObjs.container.slots.transmutation;

import it.unimi.dsi.fastutil.ints.IntList;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.InventoryContainerSlot;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotLock extends InventoryContainerSlot {
  private final TransmutationInventory inv;

  public SlotLock(TransmutationInventory inv, int index, int x, int y) {
    super(inv, index, x, y);
    this.inv = inv;
  }

  @Override
  public boolean mayPlace(@NotNull ItemStack stack) {
    return SlotPredicates.RELAY_INV.test(stack);
  }

  @NotNull
  @Override
  public ItemStack remove(int amount) {
    ItemStack stack = super.remove(amount);
    if (!stack.isEmpty() && this.inv.isServer()) {
      this.inv.syncChangedSlots(
          IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.ALL);
    }

    return stack;
  }

  @Override
  public void initialize(@NotNull ItemStack stack) {
    super.initialize(stack);
  }

  @Override
  public void set(@NotNull ItemStack stack) {
    super.set(stack);
    if (this.inv.isServer()) {
      if (stack.isEmpty()) {
        this.inv.syncChangedSlots(
            IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.ALL);
      } else {
        if (IEMCProxy.INSTANCE.hasValue(stack)) {
          this.inv.handleKnowledge(stack);
        }

        IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
        if (emcHolder != null) {
          long actualExtracted =
              emcHolder.extractEmc(
                  stack, emcHolder.getStoredEmc(stack), IEmcStorage.EmcAction.EXECUTE);
          if (actualExtracted > 0L) {
            this.inv.syncChangedSlots(
                IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.NONE);
            this.inv.addEmc(BigInteger.valueOf(actualExtracted));
          } else {
            this.inv.syncChangedSlots(
                IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.ALL);
          }
        } else {
          this.inv.syncChangedSlots(
              IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.ALL);
        }
      }
    }
  }

  @Override
  public int getMaxStackSize() {
    return 1;
  }
}
