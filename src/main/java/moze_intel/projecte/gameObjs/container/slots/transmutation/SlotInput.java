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
import moze_intel.projecte.utils.MathUtils;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotInput extends InventoryContainerSlot {
  private final TransmutationInventory inv;

  public SlotInput(TransmutationInventory inv, int index, int x, int y) {
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
          IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.IF_NEEDED);
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
          long shrunkenAvailableEMC = MathUtils.clampToLong(this.inv.provider.getEmc());
          long actualInserted =
              emcHolder.insertEmc(stack, shrunkenAvailableEMC, IEmcStorage.EmcAction.EXECUTE);
          if (actualInserted > 0L) {
            this.inv.syncChangedSlots(
                IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.NONE);
            this.inv.removeEmc(BigInteger.valueOf(actualInserted));
          } else if (emcHolder.getStoredEmc(stack) > 0L) {
            this.inv.syncChangedSlots(
                IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.ALL);
          } else {
            this.inv.syncChangedSlots(
                IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.NONE);
          }
        } else {
          this.inv.syncChangedSlots(
              IntList.of(this.getContainerSlot()), IKnowledgeProvider.TargetUpdateType.NONE);
        }
      }
    }
  }

  @Override
  public int getMaxStackSize() {
    return 1;
  }
}
