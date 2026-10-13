package moze_intel.projecte.gameObjs.container;

import java.util.Objects;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.container.slots.ComponentSlotGhost;
import moze_intel.projecte.gameObjs.container.slots.ISlotGhost;
import moze_intel.projecte.gameObjs.container.slots.InventoryContainerCopySlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MercurialEyeContainer extends PEHandContainer {
  private final ComponentSlotGhost mercurialTarget;

  public static MercurialEyeContainer fromNetwork(
      int windowId, Inventory playerInv, FriendlyByteBuf buf) {
    return new MercurialEyeContainer(
        windowId, playerInv, (InteractionHand) buf.readEnum(InteractionHand.class), buf.readByte());
  }

  public MercurialEyeContainer(
      int windowId, Inventory playerInv, InteractionHand hand, int selected) {
    super(PEContainerTypes.MERCURIAL_EYE_CONTAINER, windowId, playerInv, hand, selected);
    IItemHandler handler =
        Objects.requireNonNull(Capabilities.ItemHandler.ITEM.find(this.stack, null));
    this.addSlot(new InventoryContainerCopySlot(handler, 0, 50, 26));
    this.addSlot(this.mercurialTarget = new ComponentSlotGhost(handler, 1, 104, 26));
    this.addPlayerInventory(6, 56);
  }

  @Override
  public void clickPostValidate(
      int slotId, int button, @NotNull ContainerInput flag, @NotNull Player player) {
    if (!(this.tryGetSlot(slotId) instanceof ISlotGhost ghost && ghost.tryClear())) {
      super.clickPostValidate(slotId, button, flag, player);
    }
  }

  @NotNull
  @Override
  public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
    if (slotID > 1 && !this.mercurialTarget.hasItem()) {
      Slot currentSlot = this.tryGetSlot(slotID);
      if (currentSlot == null || !currentSlot.hasItem()) {
        return ItemStack.EMPTY;
      }

      ItemStack slotStack = currentSlot.getItem();
      if (!slotStack.isEmpty() && this.mercurialTarget.mayPlace(slotStack)) {
        return ItemStack.EMPTY;
      }
    }

    return super.quickMoveStack(player, slotID);
  }
}
