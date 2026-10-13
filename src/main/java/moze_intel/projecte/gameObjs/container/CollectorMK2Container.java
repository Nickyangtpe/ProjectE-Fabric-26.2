package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK2BlockEntity;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.entity.player.Inventory;

public class CollectorMK2Container extends CollectorMK1Container {
  public CollectorMK2Container(
      int windowId, Inventory playerInv, CollectorMK2BlockEntity collector) {
    super(PEContainerTypes.COLLECTOR_MK2_CONTAINER, windowId, playerInv, collector);
  }

  @Override
  void initSlots() {
    IItemHandler aux = this.collector.getAux();
    IItemHandler main = this.collector.getInput();
    this.addSlot(new ValidatedSlot(aux, 0, 140, 58, SlotPredicates.COLLECTOR_INV));
    int counter = 0;

    for (int i = 2; i >= 0; i--) {
      for (int j = 3; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                main, counter++, 18 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
      }
    }

    this.addSlot(new ValidatedSlot(aux, 1, 140, 13, SlotPredicates.ALWAYS_FALSE));
    this.addSlot(new SlotGhost(aux, 2, 169, 36, SlotPredicates.COLLECTOR_LOCK));
    this.addPlayerInventory(20, 84);
  }
}
