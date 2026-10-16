package moze_intel.projecte.gameObjs.container;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.container.slots.MatterFurnaceOutputSlot;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import org.jetbrains.annotations.NotNull;

public class DMFurnaceContainer extends PEContainer {
  public final DMFurnaceBlockEntity furnace;

  public DMFurnaceContainer(int windowId, Inventory playerInv, DMFurnaceBlockEntity furnace) {
    this(PEContainerTypes.DM_FURNACE_CONTAINER, windowId, playerInv, furnace);
  }

  protected DMFurnaceContainer(
      ContainerTypeRegistryObject<? extends DMFurnaceContainer> type,
      int windowId,
      Inventory playerInv,
      DMFurnaceBlockEntity furnace) {
    super(type, windowId, playerInv);
    this.furnace = furnace;
    this.initSlots();
    this.addDataSlot(() -> this.furnace.litTime, value -> this.furnace.litTime = value);
    this.addDataSlot(() -> this.furnace.litDuration, value -> this.furnace.litDuration = value);
    this.addDataSlot(
        () -> this.furnace.cookingProgress, value -> this.furnace.cookingProgress = value);
    this.addDataSlot(
        () -> this.furnace.cookingTotalTime, value -> this.furnace.cookingTotalTime = value);
  }

  private void addDataSlot(final IntSupplier getter, final IntConsumer setter) {
    this.addDataSlot(
        new DataSlot() {
          public int get() {
            return getter.getAsInt();
          }

          public void set(int value) {
            setter.accept(value);
          }
        });
  }

  void initSlots() {
    IItemHandler fuel = this.furnace.getFuel();
    IItemHandler input = this.furnace.getInput();
    IItemHandler output = this.furnace.getOutput();
    this.addSlot(new ValidatedSlot(fuel, 0, 49, 53, SlotPredicates.FURNACE_FUEL));
    this.addSlot(new ValidatedSlot(input, 0, 49, 17, this.furnace::hasSmeltingResult));
    int counter = 1;

    for (int i = 1; i >= 0; i--) {
      for (int j = 3; j >= 0; j--) {
        this.addSlot(
            new ValidatedSlot(
                input, counter++, 13 + i * 18, 8 + j * 18, this.furnace::hasSmeltingResult));
      }
    }

    counter = output.getSlots() - 1;
    this.addSlot(
        new MatterFurnaceOutputSlot(
            this.playerInv.player, this.furnace, output, counter--, 109, 35));

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 4; j++) {
        this.addSlot(
            new MatterFurnaceOutputSlot(
                this.playerInv.player, this.furnace, output, counter--, 131 + i * 18, 8 + j * 18));
      }
    }

    this.addPlayerInventory(8, 84);
  }

  public boolean stillValid(@NotNull Player player) {
    return Container.stillValidBlockEntity(this.furnace, player);
  }
}
