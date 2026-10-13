package moze_intel.projecte.gameObjs.container;

import java.util.Objects;
import java.util.function.Predicate;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity;
import moze_intel.projecte.gameObjs.container.slots.SlotCondenserLock;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import moze_intel.projecte.network.packets.to_client.container.UpdateCondenserLockPKT;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CondenserContainer extends EmcChestBlockEntityContainer<CondenserBlockEntity> {
  public static final int MAX_PROGRESS = 102;
  public final PEContainer.BoxedLong displayEmc = new PEContainer.BoxedLong();
  public final PEContainer.BoxedLong requiredEmc = new PEContainer.BoxedLong();
  @Nullable private ItemInfo lastLockInfo;

  public CondenserContainer(int windowId, Inventory playerInv, CondenserBlockEntity condenser) {
    this(PEContainerTypes.CONDENSER_CONTAINER, windowId, playerInv, condenser);
  }

  protected CondenserContainer(
      ContainerTypeRegistryObject<? extends CondenserContainer> type,
      int windowId,
      Inventory playerInv,
      CondenserBlockEntity condenser) {
    super(type, windowId, playerInv, condenser);
    this.longFields.add(this.displayEmc);
    this.longFields.add(this.requiredEmc);
    this.initSlots();
  }

  protected void initSlots() {
    this.addSlot(new SlotCondenserLock(this.blockEntity::getLockInfo, 0, 12, 6));
    Predicate<ItemStack> validator =
        s -> SlotPredicates.HAS_EMC.test(s) && !this.blockEntity.isStackEqualToLock(s);
    IItemHandler handler = this.blockEntity.getInput();

    for (int i = 0; i < 7; i++) {
      for (int j = 0; j < 13; j++) {
        this.addSlot(new ValidatedSlot(handler, j + i * 13, 12 + j * 18, 26 + i * 18, validator));
      }
    }

    this.addPlayerInventory(48, 154);
  }

  @Override
  protected void broadcastPE(boolean all) {
    this.displayEmc.set(this.blockEntity.displayEmc);
    this.requiredEmc.set(this.blockEntity.requiredEmc);
    ItemInfo lockInfo = this.blockEntity.getLockInfo();
    if (all || !Objects.equals(lockInfo, this.lastLockInfo)) {
      this.lastLockInfo = lockInfo;
      this.syncDataChange(new UpdateCondenserLockPKT((short) this.containerId, lockInfo));
    }

    super.broadcastPE(all);
  }

  public boolean stillValid(@NotNull Player player) {
    return Container.stillValidBlockEntity(this.blockEntity, player);
  }

  public void clicked(int slot, int button, @NotNull ContainerInput flag, @NotNull Player player) {
    if (slot == 0) {
      if (this.blockEntity.attemptCondenserSet(player)) {
        this.broadcastChanges();
      }
    } else {
      super.clicked(slot, button, flag, player);
    }
  }

  public int getProgressScaled() {
    if (this.requiredEmc.get() == 0L) {
      return 0;
    } else {
      return this.displayEmc.get() >= this.requiredEmc.get()
          ? 102
          : (int) (102.0 * ((double) this.displayEmc.get() / this.requiredEmc.get()));
    }
  }

  public void updateLockInfo(@Nullable ItemInfo lockInfo) {
    this.blockEntity.setLockInfoFromPacket(lockInfo);
  }
}
