package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PEEntityBlock<BLOCK_ENTITY extends BlockEntity> extends EntityBlock {
  @Nullable
  BlockEntityTypeRegistryObject<? extends BLOCK_ENTITY> getType();

  @Nullable
  default BLOCK_ENTITY newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
    BlockEntityTypeRegistryObject<? extends BLOCK_ENTITY> type = this.getType();
    return (BLOCK_ENTITY) (type == null ? null : type.get().create(pos, state));
  }

  @Nullable
  default <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      @NotNull Level level,
      @NotNull BlockState state,
      @NotNull BlockEntityType<T> blockEntityType) {
    BlockEntityTypeRegistryObject<? extends BLOCK_ENTITY> type = this.getType();
    return (BlockEntityTicker<T>)
        (type != null && blockEntityType == type.get()
            ? type.getTicker(level.isClientSide())
            : null);
  }

  default boolean triggerBlockEntityEvent(
      @NotNull BlockState state, Level level, BlockPos pos, int id, int param) {
    BlockEntity blockEntity = WorldHelper.getBlockEntity(level, pos);
    return blockEntity != null && blockEntity.triggerEvent(id, param);
  }
}
