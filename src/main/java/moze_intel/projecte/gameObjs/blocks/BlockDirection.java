package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public abstract class BlockDirection extends Block {
  public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

  public BlockDirection(Properties props) {
    super(props);
  }

  protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> props) {
    super.createBlockStateDefinition(props);
    props.add(new Property[] {FACING});
  }

  @NotNull
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    return (BlockState)
        this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
  }

  @Deprecated
  protected void affectNeighborsAfterRemoval(
      @NotNull BlockState state,
      @NotNull ServerLevel level,
      @NotNull BlockPos pos,
      boolean isMoving) {
    level.updateNeighbourForOutputSignal(pos, state.getBlock());
    super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
  }

  @Deprecated
  public void attack(
      @NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player) {
    if (!level.isClientSide()) {
      ItemStack stack = player.getMainHandItem();
      if (!stack.isEmpty() && stack.is(PEItems.PHILOSOPHERS_STONE.get())) {
        level.setBlockAndUpdate(
            pos,
            (BlockState)
                level.getBlockState(pos).setValue(FACING, player.getDirection().getOpposite()));
      }
    }
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
}
