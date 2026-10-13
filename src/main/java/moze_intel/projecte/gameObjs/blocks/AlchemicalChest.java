package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.gameObjs.block_entities.EmcChestBlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlchemicalChest extends BlockDirection
    implements SimpleWaterloggedBlock, PEEntityBlock<EmcChestBlockEntity> {
  private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

  public AlchemicalChest(Properties props) {
    super(props);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.getStateDefinition().any())
                        .setValue(FACING, Direction.NORTH))
                .setValue(BlockStateProperties.WATERLOGGED, false));
  }

  @Override
  protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> props) {
    super.createBlockStateDefinition(props);
    props.add(new Property[] {BlockStateProperties.WATERLOGGED});
  }

  @Deprecated
  public boolean isPathfindable(
      @NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
    return false;
  }

  @Deprecated
  @NotNull
  public VoxelShape getShape(
      @NotNull BlockState state,
      @NotNull BlockGetter level,
      @NotNull BlockPos pos,
      @NotNull CollisionContext ctx) {
    return SHAPE;
  }

  @Deprecated
  @NotNull
  public RenderShape getRenderShape(@NotNull BlockState state) {
    return RenderShape.INVISIBLE;
  }

  @Deprecated
  @NotNull
  protected InteractionResult useWithoutItem(
      @NotNull BlockState state,
      Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull BlockHitResult rtr) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    EmcChestBlockEntity chest =
        WorldHelper.getBlockEntity(EmcChestBlockEntity.class, level, pos, true);
    if (chest != null) {
      ContainerTypeDeferredRegister.openMenu(player, chest, pos);
      player.awardStat(Stats.OPEN_CHEST);
      if (level instanceof ServerLevel serverLevel) {
        PiglinAi.angerNearbyPiglins(serverLevel, player, true);
      }
    }

    return InteractionResult.CONSUME;
  }

  @Nullable
  @Override
  public BlockEntityTypeRegistryObject<? extends EmcChestBlockEntity> getType() {
    return PEBlockEntityTypes.ALCHEMICAL_CHEST;
  }

  @Deprecated
  public boolean triggerEvent(
      @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param) {
    super.triggerEvent(state, level, pos, id, param);
    return this.triggerBlockEntityEvent(state, level, pos, id, param);
  }

  @Deprecated
  public void tick(
      @NotNull BlockState state,
      @NotNull ServerLevel level,
      @NotNull BlockPos pos,
      @NotNull RandomSource random) {
    EmcChestBlockEntity chest = WorldHelper.getBlockEntity(EmcChestBlockEntity.class, level, pos);
    if (chest != null) {
      chest.recheckOpen();
    }
  }

  @Deprecated
  public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
    return true;
  }

  @Deprecated
  public int getAnalogOutputSignal(
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Direction direction) {
    return ItemHandlerHelper.calcRedstoneFromInventory(
        WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, pos, state, null, null));
  }

  @NotNull
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return (BlockState)
        super.getStateForPlacement(context)
            .setValue(
                BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType()
                    == Fluids.WATER);
  }

  @Deprecated
  @NotNull
  public FluidState getFluidState(BlockState state) {
    return state.getValue(BlockStateProperties.WATERLOGGED)
        ? Fluids.WATER.getSource(false)
        : super.getFluidState(state);
  }

  @Deprecated
  @NotNull
  public BlockState updateShape(
      @NotNull BlockState state,
      @NotNull LevelReader level,
      @NotNull ScheduledTickAccess tickAccess,
      @NotNull BlockPos currentPos,
      @NotNull Direction facing,
      @NotNull BlockPos facingPos,
      @NotNull BlockState facingState,
      @NotNull RandomSource random) {
    if ((Boolean) state.getValue(BlockStateProperties.WATERLOGGED)) {
      tickAccess.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }

    return super.updateShape(
        state, level, tickAccess, currentPos, facing, facingPos, facingState, random);
  }
}
