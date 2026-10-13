package moze_intel.projecte.gameObjs.container;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.gameObjs.container.slots.HotBarSlot;
import moze_intel.projecte.gameObjs.container.slots.IInsertableSlot;
import moze_intel.projecte.gameObjs.container.slots.IInventoryContainerSlot;
import moze_intel.projecte.gameObjs.container.slots.MainInventorySlot;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.container.UpdateWindowLongPKT;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PEContainer extends AbstractContainerMenu {
  protected final List<IInventoryContainerSlot> inventoryContainerSlots = new ArrayList<>();
  protected final List<MainInventorySlot> mainInventorySlots = new ArrayList<>();
  protected final List<HotBarSlot> hotBarSlots = new ArrayList<>();
  protected final List<PEContainer.BoxedLong> longFields = new ArrayList<>();
  protected final Inventory playerInv;

  protected PEContainer(
      ContainerTypeRegistryObject<? extends PEContainer> typeRO, int id, Inventory playerInv) {
    super(typeRO.get(), id);
    this.playerInv = playerInv;
  }

  protected void addPlayerInventory(int xStart, int yStart) {
    int slotSize = 18;
    int rows = 3;

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < 9; j++) {
        this.addSlot(
            this.createMainInventorySlot(
                this.playerInv, j + i * 9 + 9, xStart + j * slotSize, yStart + i * slotSize));
      }
    }

    yStart = yStart + slotSize * rows + 4;

    for (int i = 0; i < Inventory.getSelectionSize(); i++) {
      this.addSlot(this.createHotBarSlot(this.playerInv, i, xStart + i * slotSize, yStart));
    }
  }

  protected MainInventorySlot createMainInventorySlot(
      @NotNull Inventory inv, int index, int x, int y) {
    return new MainInventorySlot(inv, index, x, y);
  }

  protected HotBarSlot createHotBarSlot(@NotNull Inventory inv, int index, int x, int y) {
    return new HotBarSlot(inv, index, x, y);
  }

  @NotNull
  protected Slot addSlot(@NotNull Slot slot) {
    super.addSlot(slot);
    switch (slot) {
      case IInventoryContainerSlot containerSlot:
        this.inventoryContainerSlots.add(containerSlot);
        break;
      case MainInventorySlot inventorySlot:
        this.mainInventorySlots.add(inventorySlot);
        break;
      case HotBarSlot hotBarSlot:
        this.hotBarSlots.add(hotBarSlot);
        break;
      default:
    }

    return slot;
  }

  @Nullable
  public Slot tryGetSlot(int slotId) {
    return slotId >= 0 && slotId < this.slots.size() ? this.getSlot(slotId) : null;
  }

  @NotNull
  public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
    Slot currentSlot = this.tryGetSlot(slotID);
    if (currentSlot != null && currentSlot.hasItem()) {
      ItemStack slotStack = currentSlot.getItem();
      ItemStack stackToInsert = slotStack;
      if (currentSlot instanceof IInventoryContainerSlot) {
        stackToInsert = insertItem(this.hotBarSlots, stackToInsert, true);
        stackToInsert = insertItem(this.mainInventorySlots, stackToInsert, true);
        stackToInsert = insertItem(this.hotBarSlots, stackToInsert, false);
        stackToInsert = insertItem(this.mainInventorySlots, stackToInsert, false);
      } else {
        stackToInsert = insertItem(this.inventoryContainerSlots(), stackToInsert, true);
        if (slotStack.getCount() == stackToInsert.getCount()) {
          stackToInsert = insertItem(this.inventoryContainerSlots(), stackToInsert, false);
          if (slotStack.getCount() == stackToInsert.getCount()) {
            if (currentSlot instanceof MainInventorySlot) {
              stackToInsert = insertItem(this.hotBarSlots, stackToInsert, true);
              stackToInsert = insertItem(this.hotBarSlots, stackToInsert, false);
            } else if (currentSlot instanceof HotBarSlot) {
              stackToInsert = insertItem(this.mainInventorySlots, stackToInsert, true);
              stackToInsert = insertItem(this.mainInventorySlots, stackToInsert, false);
            }
          }
        }
      }

      return stackToInsert.getCount() == slotStack.getCount()
          ? ItemStack.EMPTY
          : this.transferSuccess(currentSlot, player, slotStack, stackToInsert);
    } else {
      return ItemStack.EMPTY;
    }
  }

  private <SLOT extends Slot & IInsertableSlot> List<SLOT> inventoryContainerSlots() {
    return (List<SLOT>) (List<?>) this.inventoryContainerSlots;
  }

  @NotNull
  protected ItemStack transferSuccess(
      @NotNull Slot currentSlot,
      @NotNull Player player,
      @NotNull ItemStack slotStack,
      @NotNull ItemStack stackToInsert) {
    int difference = slotStack.getCount() - stackToInsert.getCount();
    ItemStack newStack = currentSlot.remove(difference);
    currentSlot.onTake(player, newStack);
    return newStack;
  }

  @NotNull
  public static <SLOT extends Slot & IInsertableSlot> ItemStack insertItem(
      List<SLOT> slots, @NotNull ItemStack stack, boolean ignoreEmpty) {
    if (stack.isEmpty()) {
      return stack;
    }

    for (SLOT slot : slots) {
      if (ignoreEmpty == slot.hasItem()) {
        stack = slot.insertItem(stack, false);
        if (stack.isEmpty()) {
          break;
        }
      }
    }

    return stack;
  }

  public final void updateProgressBarLong(int idx, long data) {
    this.longFields.get(idx).set(data);
  }

  public final int longFieldCount() {
    return this.longFields.size();
  }

  protected void broadcastPE(boolean all) {
    for (int i = 0; i < this.longFields.size(); i++) {
      PEContainer.BoxedLong boxedLong = this.longFields.get(i);
      if (boxedLong.isDirty() || all) {
        this.syncDataChange(
            new UpdateWindowLongPKT((short) this.containerId, (short) i, boxedLong.get()));
      }
    }
  }

  public void broadcastChanges() {
    super.broadcastChanges();
    this.broadcastPE(false);
  }

  public void sendAllDataToRemote() {
    super.sendAllDataToRemote();
    this.broadcastPE(true);
  }

  protected void syncDataChange(IPEPacket packet) {
    if (this.playerInv.player instanceof ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, packet);
    }
  }

  public static class BoxedLong {
    private long inner;
    private boolean dirty = false;

    public long get() {
      return this.inner;
    }

    public void set(long v) {
      if (v != this.inner) {
        this.inner = v;
        this.dirty = true;
      }
    }

    public boolean isDirty() {
      boolean ret = this.dirty;
      this.dirty = false;
      return ret;
    }
  }
}
