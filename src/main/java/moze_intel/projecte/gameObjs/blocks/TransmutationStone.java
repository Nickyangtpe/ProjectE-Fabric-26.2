package moze_intel.projecte.gameObjs.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.compat.util.PESidedSuccess;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEBlockTypes;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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

public class TransmutationStone extends DirectionalBlock implements SimpleWaterloggedBlock {
  private static final VoxelShape UP_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
  private static final VoxelShape DOWN_SHAPE = Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0);
  private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0);
  private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 4.0);
  private static final VoxelShape WEST_SHAPE = Block.box(12.0, 0.0, 0.0, 16.0, 16.0, 16.0);
  private static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 4.0, 16.0, 16.0);

  public TransmutationStone(Properties props) {
    super(props);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState) this.getStateDefinition().any()).setValue(FACING, Direction.UP))
                .setValue(BlockStateProperties.WATERLOGGED, false));
  }

  @NotNull
  protected MapCodec<? extends DirectionalBlock> codec() {
    return PEBlockTypes.TRANSMUTATION_TABLE.value();
  }

  protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> props) {
    super.createBlockStateDefinition(props);
    props.add(new Property[] {FACING, BlockStateProperties.WATERLOGGED});
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
    Direction facing = (Direction) state.getValue(FACING);

    return switch (facing) {
      case DOWN -> DOWN_SHAPE;
      case NORTH -> NORTH_SHAPE;
      case SOUTH -> SOUTH_SHAPE;
      case WEST -> WEST_SHAPE;
      case EAST -> EAST_SHAPE;
      case UP -> UP_SHAPE;
      default -> throw new MatchException(null, null);
    };
  }

  @Deprecated
  @NotNull
  protected InteractionResult useWithoutItem(
      @NotNull BlockState state,
      Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull BlockHitResult rtr) {
    if (!level.isClientSide()) {
      ContainerTypeDeferredRegister.openMenu(
          player,
          new TransmutationStone.ContainerProvider(),
          payload -> payload.writeBoolean(false));
    }

    return PESidedSuccess.sidedSuccess(level.isClientSide());
  }

  @Nullable
  public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
    BlockState state = super.getStateForPlacement(context);
    return state == null
        ? null
        : (BlockState)
            ((BlockState) state.setValue(FACING, context.getClickedFace()))
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

  @Deprecated
  @NotNull
  public BlockState rotate(BlockState state, Rotation rot) {
    return (BlockState) state.setValue(FACING, rot.rotate((Direction) state.getValue(FACING)));
  }

  @Deprecated
  @NotNull
  public BlockState mirror(BlockState state, Mirror mirrorIn) {
    return state.rotate(mirrorIn.getRotation((Direction) state.getValue(FACING)));
  }

  private static class ContainerProvider implements MenuProvider {
    public AbstractContainerMenu createMenu(
        int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
      return new TransmutationContainer(windowId, playerInventory);
    }

    @NotNull
    public Component getDisplayName() {
      return PELang.TRANSMUTATION_TRANSMUTE.translate();
    }
  }
}
