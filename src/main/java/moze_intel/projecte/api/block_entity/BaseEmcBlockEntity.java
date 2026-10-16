package moze_intel.projecte.api.block_entity;

import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class BaseEmcBlockEntity extends BlockEntity implements IEmcStorage {
  public static final ICapabilityProvider<BaseEmcBlockEntity, @Nullable Direction, IEmcStorage>
      EMC_STORAGE_PROVIDER = (blockEntity, context) -> blockEntity;
  private long maximumEMC;
  private long currentEMC;

  protected BaseEmcBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
    this.setMaximumEMC(Long.MAX_VALUE);
  }

  public final void setMaximumEMC(@Range(from = 1L, to = Long.MAX_VALUE) long max) {
    this.maximumEMC = max;
    if (this.getStoredEmc() > this.getMaximumEmc()) {
      this.currentEMC = this.getMaximumEmc();
      this.storedEmcChanged();
    }
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long getStoredEmc() {
    return this.currentEMC;
  }

  @Override
  public @Range(from = 1L, to = Long.MAX_VALUE) long getMaximumEmc() {
    return this.maximumEMC;
  }

  protected @Range(from = 0L, to = Long.MAX_VALUE) long getEmcInsertLimit() {
    return this.getNeededEmc();
  }

  protected @Range(from = 0L, to = Long.MAX_VALUE) long getEmcExtractLimit() {
    return this.getStoredEmc();
  }

  protected boolean canAcceptEmc() {
    return true;
  }

  protected boolean canProvideEmc() {
    return true;
  }

  @Override
  public long extractEmc(long toExtract, IEmcStorage.EmcAction action) {
    if (toExtract < 0L) {
      return this.insertEmc(-toExtract, action);
    } else {
      return this.canProvideEmc()
          ? this.forceExtractEmc(Math.min(this.getEmcExtractLimit(), toExtract), action)
          : 0L;
    }
  }

  @Override
  public long insertEmc(long toAccept, IEmcStorage.EmcAction action) {
    if (toAccept < 0L) {
      return this.extractEmc(-toAccept, action);
    } else {
      return this.canAcceptEmc()
          ? this.forceInsertEmc(Math.min(this.getEmcInsertLimit(), toAccept), action)
          : 0L;
    }
  }

  protected long forceExtractEmc(long toExtract, IEmcStorage.EmcAction action) {
    if (toExtract < 0L) {
      return this.forceInsertEmc(-toExtract, action);
    }

    long toRemove = Math.min(this.getStoredEmc(), toExtract);
    if (action.execute()) {
      this.currentEMC -= toRemove;
      this.storedEmcChanged();
    }

    return toRemove;
  }

  protected long forceInsertEmc(long toAccept, IEmcStorage.EmcAction action) {
    if (toAccept < 0L) {
      return this.forceExtractEmc(-toAccept, action);
    }

    long toAdd = Math.min(this.getNeededEmc(), toAccept);
    if (action.execute()) {
      this.currentEMC += toAdd;
      this.storedEmcChanged();
    }

    return toAdd;
  }

  protected void storedEmcChanged() {
    this.setChanged();
  }

  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    if (this.getStoredEmc() > this.getMaximumEmc()) {
      this.currentEMC = this.getMaximumEmc();
    }

    tag.putLong("emc", this.getStoredEmc());
  }

  protected void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    this.currentEMC = Math.min(tag.getLongOr("emc", 0L), this.getMaximumEmc());
  }
}
