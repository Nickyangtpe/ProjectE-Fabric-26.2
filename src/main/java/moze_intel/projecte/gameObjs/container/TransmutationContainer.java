package moze_intel.projecte.gameObjs.container;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotConsume;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotInput;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotLock;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotOutput;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotUnlearn;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TransmutationContainer extends PEHandContainer {
  private final List<SlotInput> inputSlots = new ArrayList<>();
  public final TransmutationInventory transmutationInventory =
      new TransmutationInventory(this.playerInv.player);
  private SlotUnlearn unlearn;

  public static TransmutationContainer fromNetwork(
      int windowId, Inventory playerInv, FriendlyByteBuf buf) {
    return buf.readBoolean()
        ? new TransmutationContainer(
            windowId,
            playerInv,
            (InteractionHand) buf.readEnum(InteractionHand.class),
            buf.readByte())
        : new TransmutationContainer(windowId, playerInv);
  }

  public TransmutationContainer(int windowId, Inventory playerInv) {
    super(PEContainerTypes.TRANSMUTATION_CONTAINER, windowId, playerInv, null, 0);
    this.initSlots();
  }

  public TransmutationContainer(
      int windowId, Inventory playerInv, InteractionHand hand, int selected) {
    super(PEContainerTypes.TRANSMUTATION_CONTAINER, windowId, playerInv, hand, selected);
    this.initSlots();
  }

  private void initSlots() {
    this.addSlot(new SlotInput(this.transmutationInventory, 0, 43, 23));
    this.addSlot(new SlotInput(this.transmutationInventory, 1, 34, 41));
    this.addSlot(new SlotInput(this.transmutationInventory, 2, 52, 41));
    this.addSlot(new SlotInput(this.transmutationInventory, 3, 16, 50));
    this.addSlot(new SlotInput(this.transmutationInventory, 4, 70, 50));
    this.addSlot(new SlotInput(this.transmutationInventory, 5, 34, 59));
    this.addSlot(new SlotInput(this.transmutationInventory, 6, 52, 59));
    this.addSlot(new SlotInput(this.transmutationInventory, 7, 43, 77));
    this.addSlot(new SlotLock(this.transmutationInventory, 8, 158, 50));
    this.addSlot(new SlotConsume(this.transmutationInventory, 9, 107, 97));
    this.addSlot(this.unlearn = new SlotUnlearn(this.transmutationInventory, 10, 89, 97));
    this.addSlot(new SlotOutput(this.transmutationInventory, 11, 158, 9));
    this.addSlot(new SlotOutput(this.transmutationInventory, 12, 176, 13));
    this.addSlot(new SlotOutput(this.transmutationInventory, 13, 193, 30));
    this.addSlot(new SlotOutput(this.transmutationInventory, 14, 199, 50));
    this.addSlot(new SlotOutput(this.transmutationInventory, 15, 193, 70));
    this.addSlot(new SlotOutput(this.transmutationInventory, 16, 176, 87));
    this.addSlot(new SlotOutput(this.transmutationInventory, 17, 158, 91));
    this.addSlot(new SlotOutput(this.transmutationInventory, 18, 140, 87));
    this.addSlot(new SlotOutput(this.transmutationInventory, 19, 123, 70));
    this.addSlot(new SlotOutput(this.transmutationInventory, 20, 116, 50));
    this.addSlot(new SlotOutput(this.transmutationInventory, 21, 123, 30));
    this.addSlot(new SlotOutput(this.transmutationInventory, 22, 140, 13));
    this.addSlot(new SlotOutput(this.transmutationInventory, 23, 158, 31));
    this.addSlot(new SlotOutput(this.transmutationInventory, 24, 177, 50));
    this.addSlot(new SlotOutput(this.transmutationInventory, 25, 158, 69));
    this.addSlot(new SlotOutput(this.transmutationInventory, 26, 139, 50));
    this.addPlayerInventory(35, 117);
  }

  @NotNull
  @Override
  protected Slot addSlot(@NotNull Slot slot) {
    if (slot instanceof SlotInput input) {
      this.inputSlots.add(input);
    }

    return super.addSlot(slot);
  }

  public void removed(@NotNull Player player) {
    super.removed(player);
    if (player.isAlive()
        && !(player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected())) {
      player.getInventory().placeItemBackInInventory(this.unlearn.getItem());
    } else {
      player.drop(this.unlearn.getItem(), false);
    }
  }

  @NotNull
  @Override
  public ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
    if (slotIndex >= 9 && slotIndex != 10) {
      Slot currentSlot = this.tryGetSlot(slotIndex);
      if (currentSlot != null && currentSlot.hasItem()) {
        if (slotIndex >= 11 && slotIndex <= 26) {
          ItemStack stack = currentSlot.getItem().copy();
          long itemEmc = IEMCProxy.INSTANCE.getValue(stack);
          if (itemEmc > 0L) {
            IItemHandler destination = Capabilities.ItemHandler.ENTITY.find(player, null);
            if (destination == null) {
              return ItemStack.EMPTY;
            }

            BigInteger emcPerItem = BigInteger.valueOf(itemEmc);
            BigInteger affordable =
                this.transmutationInventory.getAvailableEmc().divide(emcPerItem);
            if (affordable.signum() <= 0) {
              return ItemStack.EMPTY;
            }

            int plannedCount =
                affordable.min(BigInteger.valueOf(stack.getMaxStackSize())).intValue();
            ItemStack planned = stack.copyWithCount(plannedCount);
            ItemStack simulatedRemainder =
                ItemHandlerHelper.insertItemStacked(destination, planned.copy(), true);
            int simulatedAccepted = plannedCount - simulatedRemainder.getCount();
            if (simulatedAccepted <= 0) {
              return ItemStack.EMPTY;
            }

            ItemStack insertionRemainder =
                ItemHandlerHelper.insertItemStacked(
                    destination, planned.copyWithCount(simulatedAccepted), false);
            int insertedCount = simulatedAccepted - insertionRemainder.getCount();
            if (insertedCount > 0 && this.transmutationInventory.isServer()) {
              this.transmutationInventory.removeEmc(
                  emcPerItem.multiply(BigInteger.valueOf(insertedCount)));
            }
          }
        } else if (slotIndex > 26) {
          ItemStack slotStack = currentSlot.getItem();
          ItemStack stackToInsert = slotStack;
          if (PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stackToInsert, null) != null) {
            stackToInsert = insertItem(this.inputSlots, stackToInsert, true);
            if (slotStack.getCount() == stackToInsert.getCount()) {
              stackToInsert = insertItem(this.inputSlots, stackToInsert, false);
            }

            if (slotStack.getCount() != stackToInsert.getCount()) {
              return this.transferSuccess(currentSlot, player, slotStack, stackToInsert);
            }
          }

          long emc = IEMCProxy.INSTANCE.getSellValue(stackToInsert);
          if (emc > 0L || stackToInsert.getItem() instanceof Tome) {
            if (this.transmutationInventory.isServer()) {
              BigInteger emcBigInt = BigInteger.valueOf(emc);
              this.transmutationInventory.handleKnowledge(stackToInsert);
              this.transmutationInventory.addEmc(
                  emcBigInt.multiply(BigInteger.valueOf(stackToInsert.getCount())));
            }

            currentSlot.set(ItemStack.EMPTY);
          }
        }

        return ItemStack.EMPTY;
      } else {
        return ItemStack.EMPTY;
      }
    } else {
      return super.quickMoveStack(player, slotIndex);
    }
  }

  @Override
  public void clickPostValidate(
      int slotIndex, int dragType, @NotNull ContainerInput clickType, @NotNull Player player) {
    if (player.level().isClientSide()
        && slotIndex >= 0
        && slotIndex < this.transmutationInventory.getSlots()
        && this.transmutationInventory.getHandlerForSlot(slotIndex)
            == this.transmutationInventory.outputs) {
      Slot slot = this.tryGetSlot(slotIndex);
      if (slot != null) {
        PacketDistributor.sendToServer(
            new SearchUpdatePKT(
                this.transmutationInventory.getIndexFromSlot(slotIndex), slot.getItem()));
      }
    }

    super.clickPostValidate(slotIndex, dragType, clickType, player);
  }

  public boolean canDragTo(@NotNull Slot slot) {
    return !(slot instanceof SlotConsume)
        && !(slot instanceof SlotUnlearn)
        && !(slot instanceof SlotInput)
        && !(slot instanceof SlotLock)
        && !(slot instanceof SlotOutput);
  }
}
