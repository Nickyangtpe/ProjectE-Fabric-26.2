package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.RelayMK1BlockEntity;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import org.jetbrains.annotations.NotNull;

public class RelayMK1Container extends PEContainer {
  public final RelayMK1BlockEntity relay;
  private final DataSlot kleinChargeProgress = DataSlot.standalone();
  private final DataSlot inputBurnProgress = DataSlot.standalone();
  public final PEContainer.BoxedLong emc = new PEContainer.BoxedLong();

  public RelayMK1Container(int windowId, Inventory playerInv, RelayMK1BlockEntity relay) {
    this(PEContainerTypes.RELAY_MK1_CONTAINER, windowId, playerInv, relay);
  }

  protected RelayMK1Container(
      ContainerTypeRegistryObject<? extends RelayMK1Container> type,
      int windowId,
      Inventory playerInv,
      RelayMK1BlockEntity relay) {
    super(type, windowId, playerInv);
    this.longFields.add(this.emc);
    this.addDataSlot(this.kleinChargeProgress);
    this.addDataSlot(this.inputBurnProgress);
    this.relay = relay;
    this.initSlots();
  }

  void initSlots() {
    IItemHandler input = this.relay.getInput();
    IItemHandler output = this.relay.getOutput();
    this.addSlot(new ValidatedSlot(output, 0, 127, 43, SlotPredicates.EMC_HOLDER));
    this.addSlot(new ValidatedSlot(input, 0, 67, 43, SlotPredicates.RELAY_INV));
    int counter = 1;

    for (int i = 1; i >= 0; i--) {
      for (int j = 2; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                input, counter++, 27 + i * 18, 17 + j * 18, SlotPredicates.RELAY_INV));
      }
    }

    this.addPlayerInventory(8, 95);
  }

  @Override
  protected void broadcastPE(boolean all) {
    this.emc.set(this.relay.getStoredEmc());
    this.kleinChargeProgress.set((int) (this.relay.getItemChargeProportion() * 8000.0));
    this.inputBurnProgress.set((int) (this.relay.getInputBurnProportion() * 8000.0));
    super.broadcastPE(all);
  }

  public boolean stillValid(@NotNull Player player) {
    return Container.stillValidBlockEntity(this.relay, player);
  }

  public double getKleinChargeProgress() {
    return this.kleinChargeProgress.get() / 8000.0;
  }

  public double getInputBurnProgress() {
    return this.inputBurnProgress.get() / 8000.0;
  }
}
