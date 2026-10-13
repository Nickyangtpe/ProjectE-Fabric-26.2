package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity;
import moze_intel.projecte.gameObjs.container.slots.ISlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import org.jetbrains.annotations.NotNull;

public class CollectorMK1Container extends PEContainer {
  public final CollectorMK1BlockEntity collector;
  public final DataSlot sunLevel = DataSlot.standalone();
  public final PEContainer.BoxedLong emc = new PEContainer.BoxedLong();
  private final DataSlot kleinChargeProgress = DataSlot.standalone();
  private final DataSlot fuelProgress = DataSlot.standalone();
  public final PEContainer.BoxedLong kleinEmc = new PEContainer.BoxedLong();

  public CollectorMK1Container(
      int windowId, Inventory playerInv, CollectorMK1BlockEntity collector) {
    this(PEContainerTypes.COLLECTOR_MK1_CONTAINER, windowId, playerInv, collector);
  }

  protected CollectorMK1Container(
      ContainerTypeRegistryObject<? extends CollectorMK1Container> type,
      int windowId,
      Inventory playerInv,
      CollectorMK1BlockEntity collector) {
    super(type, windowId, playerInv);
    this.longFields.add(this.emc);
    this.addDataSlot(this.sunLevel);
    this.addDataSlot(this.kleinChargeProgress);
    this.addDataSlot(this.fuelProgress);
    this.longFields.add(this.kleinEmc);
    this.collector = collector;
    this.initSlots();
  }

  void initSlots() {
    IItemHandler aux = this.collector.getAux();
    IItemHandler main = this.collector.getInput();
    this.addSlot(new ValidatedSlot(aux, 0, 124, 58, SlotPredicates.COLLECTOR_INV));
    int counter = 0;

    for (int i = 1; i >= 0; i--) {
      for (int j = 3; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                main, counter++, 20 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
      }
    }

    this.addSlot(new ValidatedSlot(aux, 1, 124, 13, SlotPredicates.ALWAYS_FALSE));
    this.addSlot(new SlotGhost(aux, 2, 153, 36, SlotPredicates.COLLECTOR_LOCK));
    this.addPlayerInventory(8, 84);
  }

  public void clicked(
      int slotID, int button, @NotNull ContainerInput flag, @NotNull Player player) {
    if (!(this.tryGetSlot(slotID) instanceof ISlotGhost ghost && ghost.tryClear())) {
      super.clicked(slotID, button, flag, player);
    }
  }

  @Override
  protected void broadcastPE(boolean all) {
    this.emc.set(this.collector.getStoredEmc());
    this.sunLevel.set(this.collector.getSunLevel());
    this.kleinChargeProgress.set((int) (this.collector.getItemChargeProportion() * 8000.0));
    this.fuelProgress.set((int) (this.collector.getFuelProgress() * 8000.0));
    this.kleinEmc.set(this.collector.getItemCharge());
    super.broadcastPE(all);
  }

  public boolean stillValid(@NotNull Player player) {
    return Container.stillValidBlockEntity(this.collector, player);
  }

  public double getKleinChargeProgress() {
    return this.kleinChargeProgress.get() / 8000.0;
  }

  public double getFuelProgress() {
    return this.fuelProgress.get() / 8000.0;
  }
}
