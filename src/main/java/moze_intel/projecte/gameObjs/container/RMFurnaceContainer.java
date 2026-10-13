package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.RMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.container.slots.MatterFurnaceOutputSlot;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.entity.player.Inventory;

public class RMFurnaceContainer extends DMFurnaceContainer {
  public RMFurnaceContainer(int windowId, Inventory playerInv, RMFurnaceBlockEntity furnace) {
    super(PEContainerTypes.RM_FURNACE_CONTAINER, windowId, playerInv, furnace);
  }

  @Override
  void initSlots() {
    IItemHandler fuel = this.furnace.getFuel();
    IItemHandler input = this.furnace.getInput();
    IItemHandler output = this.furnace.getOutput();
    this.addSlot(new ValidatedSlot(fuel, 0, 65, 53, SlotPredicates.FURNACE_FUEL));
    this.addSlot(new ValidatedSlot(input, 0, 65, 17, this.furnace::hasSmeltingResult));
    int counter = 1;

    for (int i = 2; i >= 0; i--) {
      for (int j = 3; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                input, counter++, 11 + i * 18, 8 + j * 18, this.furnace::hasSmeltingResult));
      }
    }

    counter = output.getSlots() - 1;
    this.addSlot(
        new MatterFurnaceOutputSlot(
            this.playerInv.player, this.furnace, output, counter--, 125, 35));

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 4; j++) {
        this.addSlot(
            new MatterFurnaceOutputSlot(
                this.playerInv.player, this.furnace, output, counter--, 147 + i * 18, 8 + j * 18));
      }
    }

    this.addPlayerInventory(24, 84);
  }
}
