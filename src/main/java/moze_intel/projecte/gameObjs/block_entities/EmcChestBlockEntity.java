package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.container.EmcChestBlockEntityContainer;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public abstract class EmcChestBlockEntity extends EmcBlockEntity
    implements LidBlockEntity, MenuProvider, WorldlyContainer {
  protected abstract IItemHandlerModifiable getContainerInventory();

  protected IItemHandler getAutomationInventory(@Nullable Direction side) {
    return getContainerInventory();
  }

  @Override
  public int getContainerSize() {
    return getContainerInventory().getSlots();
  }

  @Override
  public boolean isEmpty() {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      if (!getItem(slot).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public ItemStack getItem(int slot) {
    return getContainerInventory().getStackInSlot(slot);
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    if (amount <= 0) {
      return ItemStack.EMPTY;
    }
    // Native hoppers retain this stack reference and restore its count if the target is full.
    // Splitting in place preserves that rollback, unlike IItemHandler's copy-on-extraction.
    ItemStack stack = getItem(slot);
    ItemStack removed = stack.split(amount);
    if (!removed.isEmpty()) {
      getContainerInventory().setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }
    return removed;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    ItemStack result = getItem(slot);
    getContainerInventory().setStackInSlot(slot, ItemStack.EMPTY);
    return result;
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    int limit = Math.min(getContainerInventory().getSlotLimit(slot), stack.getMaxStackSize());
    getContainerInventory().setStackInSlot(
        slot, stack.getCount() > limit ? stack.copyWithCount(limit) : stack);
  }

  @Override
  public void clearContent() {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      getContainerInventory().setStackInSlot(slot, ItemStack.EMPTY);
    }
  }

  @Override
  public boolean stillValid(Player player) {
    return Container.stillValidBlockEntity(this, player);
  }

  private boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
    return !isRemoved()
        && !stack.isEmpty()
        && getAutomationInventory(side).insertItem(slot, stack.copyWithCount(1), true).isEmpty();
  }

  private boolean canExtract(int slot, ItemStack stack, @Nullable Direction side) {
    return !isRemoved()
        && !stack.isEmpty()
        && !getAutomationInventory(side).extractItem(slot, 1, true).isEmpty();
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack stack) {
    return canInsert(slot, stack, null);
  }

  @Override
  public boolean canTakeItem(Container target, int slot, ItemStack stack) {
    return canExtract(slot, stack, null);
  }

  @Override
  public int[] getSlotsForFace(Direction side) {
    return java.util.stream.IntStream.range(0, getContainerSize()).toArray();
  }

  @Override
  public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
    return canInsert(slot, stack, side);
  }

  @Override
  public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
    return canExtract(slot, stack, side);
  }

  private final ContainerOpenersCounter openersCounter =
      new ContainerOpenersCounter() {
        protected void onOpen(
            @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
          level.playSound(
              null,
              pos.getX() + 0.5,
              pos.getY() + 0.5,
              pos.getZ() + 0.5,
              SoundEvents.CHEST_OPEN,
              SoundSource.BLOCKS,
              0.5F,
              level.getRandom().nextFloat() * 0.1F + 0.9F);
        }

        protected void onClose(
            @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
          level.playSound(
              null,
              pos.getX() + 0.5,
              pos.getY() + 0.5,
              pos.getZ() + 0.5,
              SoundEvents.CHEST_CLOSE,
              SoundSource.BLOCKS,
              0.5F,
              level.getRandom().nextFloat() * 0.1F + 0.9F);
        }

        protected void openerCountChanged(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            int oldCount,
            int openCount) {
          level.blockEvent(pos, state.getBlock(), 1, openCount);
        }

        public boolean isOwnContainer(Player player) {
          return player.containerMenu instanceof EmcChestBlockEntityContainer<?> container
              && container.blockEntityMatches(EmcChestBlockEntity.this);
        }
      };
  private final ChestLidController chestLidController = new ChestLidController();

  protected EmcChestBlockEntity(
      BlockEntityTypeRegistryObject<? extends EmcChestBlockEntity> type,
      BlockPos pos,
      BlockState state) {
    super(type, pos, state);
  }

  protected EmcChestBlockEntity(
      BlockEntityTypeRegistryObject<? extends EmcChestBlockEntity> type,
      BlockPos pos,
      BlockState state,
      @Range(from = 1L, to = Long.MAX_VALUE) long maxAmount) {
    super(type, pos, state, maxAmount);
  }

  public static void lidAnimateTick(
      Level level, BlockPos pos, BlockState state, EmcChestBlockEntity chest) {
    chest.chestLidController.tickLid();
  }

  public boolean triggerEvent(int id, int type) {
    if (id == 1) {
      this.chestLidController.shouldBeOpen(type > 0);
      return true;
    } else {
      return super.triggerEvent(id, type);
    }
  }

  public void startOpen(Player player) {
    if (!this.isRemoved() && !player.isSpectator() && this.level != null) {
      this.openersCounter.incrementOpeners(
          player, this.level, this.getBlockPos(), this.getBlockState(), 5.0);
    }
  }

  public void stopOpen(Player player) {
    if (!this.isRemoved() && !player.isSpectator() && this.level != null) {
      this.openersCounter.decrementOpeners(
          player, this.level, this.getBlockPos(), this.getBlockState());
    }
  }

  public void recheckOpen() {
    if (!this.isRemoved() && this.level != null) {
      this.openersCounter.recheckOpeners(this.level, this.getBlockPos(), this.getBlockState());
    }
  }

  public float getOpenNess(float partialTicks) {
    return this.chestLidController.getOpenness(partialTicks);
  }
}
