package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.gameObjs.container.slots.HotBarSlot;
import moze_intel.projecte.gameObjs.container.slots.InventoryContainerSlot;
import moze_intel.projecte.gameObjs.container.slots.MainInventorySlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AlchBagContainer extends PEHandContainer {
  private final boolean immutable;

  public static AlchBagContainer fromNetwork(
      int windowId, Inventory playerInv, FriendlyByteBuf buf) {
    return new AlchBagContainer(
        windowId,
        playerInv,
        (InteractionHand) buf.readEnum(InteractionHand.class),
        new ItemStackHandler(104),
        buf.readByte(),
        buf.readBoolean());
  }

  public AlchBagContainer(
      int windowId,
      Inventory playerInv,
      InteractionHand hand,
      IItemHandlerModifiable invBag,
      int selected,
      boolean immutable) {
    super(PEContainerTypes.ALCH_BAG_CONTAINER, windowId, playerInv, hand, selected);
    this.immutable = immutable;

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        this.addSlot(this.createContainerSlot(invBag, j + i * 13, 12 + j * 18, 5 + i * 18));
      }
    }

    this.addPlayerInventory(48, 152);
  }

  private InventoryContainerSlot createContainerSlot(
      IItemHandlerModifiable inv, int index, int x, int y) {
    return this.immutable
        ? new InventoryContainerSlot(inv, index, x, y) {
          @Override
          public boolean mayPickup(@NotNull Player player) {
            return false;
          }

          @Override
          public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
          }
        }
        : new InventoryContainerSlot(inv, index, x, y);
  }

  @Override
  protected MainInventorySlot createMainInventorySlot(
      @NotNull Inventory inv, int index, int x, int y) {
    return this.immutable
        ? new MainInventorySlot(inv, index, x, y) {
          public boolean mayPickup(@NotNull Player player) {
            return false;
          }

          public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
          }
        }
        : super.createMainInventorySlot(inv, index, x, y);
  }

  @Override
  protected HotBarSlot createHotBarSlot(@NotNull Inventory inv, int index, int x, int y) {
    return this.immutable
        ? new HotBarSlot(inv, index, x, y) {
          public boolean mayPickup(@NotNull Player player) {
            return false;
          }

          public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
          }
        }
        : super.createHotBarSlot(inv, index, x, y);
  }

  public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot) {
    return !this.immutable && super.canTakeItemForPickAll(stack, slot);
  }

  public boolean canDragTo(@NotNull Slot slot) {
    return !this.immutable && super.canDragTo(slot);
  }

  @NotNull
  @Override
  public ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
    return this.immutable ? ItemStack.EMPTY : super.quickMoveStack(player, slotIndex);
  }

  @Override
  public void clicked(
      int slotId, int dragType, @NotNull ContainerInput clickType, @NotNull Player player) {
    if (!this.immutable) {
      super.clicked(slotId, dragType, clickType, player);
    }
  }
}
