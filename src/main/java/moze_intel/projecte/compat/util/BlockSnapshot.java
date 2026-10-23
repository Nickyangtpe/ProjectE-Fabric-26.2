package moze_intel.projecte.compat.util;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockSnapshot {
  private final Level level;
  private final BlockPos pos;
  private final BlockState state;
  private final int flags;
  private final CompoundTag blockEntityData;
  private final List<ItemStack> deferredDrops = new ArrayList<>();

  public BlockSnapshot(Level level, BlockPos pos, BlockState state, int flags) {
    this.level = level;
    this.pos = pos.immutable();
    this.state = state;
    this.flags = flags;
    BlockEntity blockEntity = level.getBlockEntity(pos);
    this.blockEntityData =
        blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
  }

  public Level getLevel() {
    return this.level;
  }

  public BlockPos getPos() {
    return this.pos;
  }

  public BlockState getState() {
    return this.state;
  }

  public int getFlags() {
    return this.flags;
  }

  void deferDrops(List<ItemStack> drops) {
    this.deferredDrops.addAll(drops);
  }

  public void dropContents() {
    WorldHelper.createLootDrop(this.deferredDrops, this.level, Vec3.atCenterOf(this.pos));
    this.deferredDrops.clear();
  }

  public boolean restore(int restoreFlags) {
    boolean restored = this.level.setBlock(this.pos, this.state, restoreFlags);
    if (this.blockEntityData != null && this.state.hasBlockEntity()) {
      BlockEntity restoredBlockEntity =
          BlockEntity.loadStatic(
              this.pos, this.state, this.blockEntityData, this.level.registryAccess());
      if (restoredBlockEntity != null) {
        this.level.setBlockEntity(restoredBlockEntity);
      }
    }

    this.deferredDrops.clear();
    return restored;
  }
}
