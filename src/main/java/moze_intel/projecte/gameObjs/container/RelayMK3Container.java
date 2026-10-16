package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.RelayMK3BlockEntity;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.entity.player.Inventory;

public class RelayMK3Container extends RelayMK1Container {
  public RelayMK3Container(int windowId, Inventory playerInv, RelayMK3BlockEntity relay) {
    super(PEContainerTypes.RELAY_MK3_CONTAINER, windowId, playerInv, relay);
  }

  @Override
  void initSlots() {
    IItemHandler input = this.relay.getInput();
    IItemHandler output = this.relay.getOutput();
    this.addSlot(new ValidatedSlot(output, 0, 164, 58, SlotPredicates.EMC_HOLDER));
    this.addSlot(new ValidatedSlot(input, 0, 104, 58, SlotPredicates.RELAY_INV));
    int counter = 1;

    for (int i = 3; i >= 0; i--) {
      for (int j = 4; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                input, counter++, 28 + i * 18, 18 + j * 18, SlotPredicates.RELAY_INV));
      }
    }

    this.addPlayerInventory(26, 113);
  }
}
