package moze_intel.projecte.gameObjs.block_entities;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.block_entity.BaseEmcBlockEntity;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.util.BlockSnapshotCapture;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public abstract class EmcBlockEntity extends BaseEmcBlockEntity {
  private boolean updateComparators;
  private long lastSave;

  public EmcBlockEntity(
      BlockEntityTypeRegistryObject<? extends EmcBlockEntity> type,
      BlockPos pos,
      BlockState state) {
    this(type, pos, state, Long.MAX_VALUE);
  }

  public EmcBlockEntity(
      BlockEntityTypeRegistryObject<? extends EmcBlockEntity> type,
      BlockPos pos,
      BlockState state,
      @Range(from = 1L, to = Long.MAX_VALUE) long maxAmount) {
    super(type.get(), pos, state);
    this.setMaximumEMC(maxAmount);
  }

  protected List<IItemHandlerModifiable> getInventoriesForDrops() {
    return List.of();
  }

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (this.level instanceof ServerLevel serverLevel && !this.isRemoved()) {
      List<ItemStack> drops = new ArrayList<>();
      for (IItemHandlerModifiable inventory : this.getInventoriesForDrops()) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
          ItemStack stack = inventory.getStackInSlot(slot);
          if (!stack.isEmpty()) {
            drops.add(stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
          }
        }
      }
      // Removed block entities cannot be queried from affectNeighborsAfterRemoval.
      // Snapshot replacements defer these drops until the placement event succeeds.
      if (!BlockSnapshotCapture.deferDrops(serverLevel, pos, drops)) {
        WorldHelper.createLootDrop(drops, serverLevel, Vec3.atCenterOf(pos));
      }
    }
    // Native Container handling now sees an empty inventory, so contents drop only once.
    super.preRemoveSideEffects(pos, state);
  }

  protected void updateComparators(@NotNull Level level, @NotNull BlockPos pos) {
    if (this.updateComparators) {
      BlockState state = this.getBlockState();
      if (!state.isAir()) {
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
      }

      this.updateComparators = false;
    }
  }

  protected boolean emcAffectsComparators() {
    return false;
  }

  @Override
  protected void storedEmcChanged() {
    if (this.level != null) {
      this.markDirty(this.level, this.worldPosition, this.emcAffectsComparators());
    }
  }

  public final void setChanged() {
    if (this.level != null) {
      this.markDirty(this.level, this.worldPosition, true);
    }
  }

  public void markDirty(@NotNull Level level, @NotNull BlockPos pos, boolean recheckComparators) {
    long time = level.getGameTime();
    if (this.lastSave != time) {
      ChunkAccess chunk =
          level.getChunk(
              SectionPos.blockToSectionCoord(pos.getX()),
              SectionPos.blockToSectionCoord(pos.getZ()),
              ChunkStatus.FULL,
              false);
      if (chunk != null) {
        chunk.markUnsaved();
      }

      this.lastSave = time;
    }

    if (recheckComparators && !level.isClientSide()) {
      this.updateComparators = true;
    }
  }

  @NotNull
  public final CompoundTag getUpdateTag(@NotNull Provider registries) {
    return this.saveWithoutMetadata(registries);
  }

  public final ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  protected @Range(from = 0L, to = Long.MAX_VALUE) long sendToAllAcceptors(
      @NotNull Level level, BlockPos pos, long emc) {
    if (emc != 0L && this.canProvideEmc()) {
      emc = Math.min(this.getEmcExtractLimit(), emc);
      long sentEmc = 0L;
      List<IEmcStorage> targets = new ArrayList<>();

      for (Direction dir : Constants.DIRECTIONS) {
        IEmcStorage theirEmcStorage =
            WorldHelper.getCapability(
                level, PECapabilities.EMC_STORAGE_CAPABILITY, pos.relative(dir), dir.getOpposite());
        if (theirEmcStorage != null
            && (!this.isRelay() || !theirEmcStorage.isRelay())
            && theirEmcStorage.insertEmc(1L, IEmcStorage.EmcAction.SIMULATE) > 0L) {
          targets.add(theirEmcStorage);
        }
      }

      if (!targets.isEmpty()) {
        long emcPer = emc / targets.size();

        for (IEmcStorage target : targets) {
          long emcCanProvide = this.extractEmc(emcPer, IEmcStorage.EmcAction.SIMULATE);
          long acceptedEmc = target.insertEmc(emcCanProvide, IEmcStorage.EmcAction.EXECUTE);
          this.extractEmc(acceptedEmc, IEmcStorage.EmcAction.EXECUTE);
          sentEmc += acceptedEmc;
        }
      }

      return sentEmc;
    } else {
      return 0L;
    }
  }

  protected class CompactableStackHandler extends EmcBlockEntity.StackHandler {
    private boolean needsCompacting = true;
    private boolean empty;

    protected CompactableStackHandler(int size) {
      super(size);
    }

    @Override
    protected void onContentsChanged(int slot) {
      super.onContentsChanged(slot);
      this.needsCompacting = true;
    }

    public void compact() {
      if (this.needsCompacting) {
        if (EmcBlockEntity.this.level != null && !EmcBlockEntity.this.level.isClientSide()) {
          this.empty = ItemHelper.compactInventory(this);
        }

        this.needsCompacting = false;
      }
    }

    @Override
    protected void onLoad() {
      super.onLoad();
      this.empty = true;
      int slot = 0;

      for (int slots = this.getSlots(); slot < slots; slot++) {
        if (!this.getStackInSlot(slot).isEmpty()) {
          this.empty = false;
          break;
        }
      }
    }

    public boolean isEmpty() {
      return this.empty;
    }
  }

  protected class StackHandler extends ItemStackHandler {
    protected StackHandler(int size) {
      super(size);
    }

    @Override
    protected void onContentsChanged(int slot) {
      super.onContentsChanged(slot);
      EmcBlockEntity.this.setChanged();
    }
  }
}
