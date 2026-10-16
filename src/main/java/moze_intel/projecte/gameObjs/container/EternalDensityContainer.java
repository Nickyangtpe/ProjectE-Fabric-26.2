package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.components.GemData;
import moze_intel.projecte.gameObjs.container.inventory.EternalDensityInventory;
import moze_intel.projecte.gameObjs.container.slots.ISlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EternalDensityContainer extends PEHandContainer {
  private final EternalDensityInventory inventory;

  public static EternalDensityContainer fromNetwork(
      int windowId, Inventory playerInv, FriendlyByteBuf data) {
    return new EternalDensityContainer(
        windowId,
        playerInv,
        (InteractionHand) data.readEnum(InteractionHand.class),
        data.readByte(),
        true);
  }

  public EternalDensityContainer(
      int windowId, Inventory playerInv, InteractionHand hand, int selected) {
    this(windowId, playerInv, hand, selected, false);
  }

  private EternalDensityContainer(
      int windowId, Inventory playerInv, InteractionHand hand, int selected, boolean remote) {
    super(PEContainerTypes.ETERNAL_DENSITY_CONTAINER, windowId, playerInv, hand, selected);
    this.inventory = new EternalDensityInventory(this.stack, remote);

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        this.addSlot(
            new SlotGhost(
                this.inventory, j + i * 3, 62 + j * 18, 26 + i * 18, SlotPredicates.HAS_EMC));
      }
    }

    this.addPlayerInventory(8, 93);
  }

  public boolean isWhitelistMode() {
    return ((GemData)
            this.getStack().getOrDefault(PEDataComponentTypes.GEM_DATA.get(), GemData.EMPTY))
        .isWhitelist();
  }

  @NotNull
  @Override
  public ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
    if (slotIndex > 8) {
      Slot slot = this.tryGetSlot(slotIndex);
      if (slot != null) {
        ItemHandlerHelper.insertItem(this.inventory, slot.getItem().copyWithCount(1), false);
      }
    }

    return ItemStack.EMPTY;
  }

  @Override
  public void clickPostValidate(
      int slotIndex, int button, @NotNull ContainerInput flag, @NotNull Player player) {
    if (!(this.tryGetSlot(slotIndex) instanceof ISlotGhost ghost && ghost.tryClear())) {
      super.clickPostValidate(slotIndex, button, flag, player);
    }
  }

  public boolean canDragTo(@NotNull Slot slot) {
    return false;
  }
}
