package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK3BlockEntity;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.entity.player.Inventory;

public class CollectorMK3Container extends CollectorMK1Container {
  public CollectorMK3Container(
      int windowId, Inventory playerInv, CollectorMK3BlockEntity collector) {
    super(PEContainerTypes.COLLECTOR_MK3_CONTAINER, windowId, playerInv, collector);
  }

  @Override
  void initSlots() {
    IItemHandler aux = this.collector.getAux();
    IItemHandler main = this.collector.getInput();
    this.addSlot(new ValidatedSlot(aux, 0, 158, 58, SlotPredicates.COLLECTOR_INV));
    int counter = 0;

    for (int i = 3; i >= 0; i--) {
      for (int j = 3; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                main, counter++, 18 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
      }
    }

    this.addSlot(new ValidatedSlot(aux, 1, 158, 13, SlotPredicates.ALWAYS_FALSE));
    this.addSlot(new SlotGhost(aux, 2, 187, 36, SlotPredicates.COLLECTOR_LOCK));
    this.addPlayerInventory(30, 84);
  }
}
