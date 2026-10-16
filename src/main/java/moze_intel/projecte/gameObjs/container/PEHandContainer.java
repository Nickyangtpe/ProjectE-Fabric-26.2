package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.gameObjs.container.slots.HotBarSlot;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.network.packets.to_client.container.SyncOffhandPkt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PEHandContainer extends PEContainer {
  public final InteractionHand hand;
  private final int selected;
  protected final ItemStack stack;
  private ItemStack remoteOffHand = ItemStack.EMPTY;

  protected PEHandContainer(
      ContainerTypeRegistryObject<? extends PEHandContainer> typeRO,
      int windowId,
      Inventory playerInv,
      InteractionHand hand,
      int selected) {
    super(typeRO, windowId, playerInv);
    this.hand = hand;
    this.selected = selected;
    this.stack = this.getStack();
  }

  protected ItemStack getStack() {
    return getStack(this.playerInv, this.hand, this.selected);
  }

  public static ItemStack getStack(Inventory playerInv, InteractionHand hand, int selected) {
    if (hand == null) {
      return ItemStack.EMPTY;
    } else {
      return hand == InteractionHand.OFF_HAND
          ? playerInv.player.getOffhandItem()
          : playerInv.getItem(selected);
    }
  }

  @Override
  protected void broadcastPE(boolean all) {
    super.broadcastPE(all);
    if (this.hand == InteractionHand.OFF_HAND) {
      ItemStack offhandStack = this.getStack();
      if (!ItemStack.matches(offhandStack, this.remoteOffHand)) {
        this.remoteOffHand = offhandStack.copy();
        this.syncDataChange(new SyncOffhandPkt((short) this.containerId, this.remoteOffHand));
      }
    }
  }

  @Override
  protected HotBarSlot createHotBarSlot(@NotNull Inventory inv, int index, int x, int y) {
    return this.hand == InteractionHand.MAIN_HAND && index == this.selected
        ? new HotBarSlot(inv, index, x, y) {
          public boolean mayPickup(@NotNull Player player) {
            return false;
          }
        }
        : super.createHotBarSlot(inv, index, x, y);
  }

  public boolean stillValid(@NotNull Player player) {
    return this.hand == null
        || !this.stack.isEmpty() && player.getItemInHand(this.hand).is(this.stack.getItem());
  }

  public void clicked(
      int slotId, int dragType, @NotNull ContainerInput clickType, @NotNull Player player) {
    if (clickType == ContainerInput.SWAP) {
      if (this.hand == InteractionHand.OFF_HAND && dragType == 40) {
        return;
      }

      if (this.hand == InteractionHand.MAIN_HAND
          && dragType >= 0
          && dragType < Inventory.getSelectionSize()
          && !this.hotBarSlots.get(dragType).mayPickup(player)) {
        return;
      }
    }

    this.clickPostValidate(slotId, dragType, clickType, player);
  }

  public void clickPostValidate(
      int slotId, int dragType, @NotNull ContainerInput clickType, @NotNull Player player) {
    super.clicked(slotId, dragType, clickType, player);
  }
}
