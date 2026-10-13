package moze_intel.projecte.compat.common;

import moze_intel.projecte.compat.registries.BlockTransformationMappings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public final class ToolActionHelper {
  private ToolActionHelper() {}

  @Nullable
  public static BlockState getToolModifiedState(
      BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
    Level level = context.getLevel();
    BlockPos pos = context.getClickedPos();
    Block block = state.getBlock();
    if (ability == ItemAbilities.AXE_STRIP) {
      Block stripped = (Block) AxeItem.STRIPPABLES.get(block);
      return stripped == null ? null : stripped.withPropertiesOf(state);
    }

    if (ability == ItemAbilities.AXE_SCRAPE) {
      return BlockTransformationMappings.previousOxidationStage(state);
    }

    if (ability == ItemAbilities.AXE_WAX_OFF) {
      Block unwaxed = BlockTransformationMappings.unwaxedBlock(block);
      return unwaxed == null ? null : unwaxed.withPropertiesOf(state);
    }

    if (ability == ItemAbilities.SHOVEL_FLATTEN) {
      return (BlockState) ShovelItem.FLATTENABLES.get(block);
    }

    if (ability == ItemAbilities.HOE_TILL) {
      if (block == Blocks.ROOTED_DIRT) {
        if (!simulate && !level.isClientSide()) {
          Block.popResourceFromFace(
              level, pos, context.getClickedFace(), new ItemStack(Items.HANGING_ROOTS));
        }

        return Blocks.DIRT.defaultBlockState();
      }

      if ((block == Blocks.GRASS_BLOCK
              || block == Blocks.DIRT_PATH
              || block == Blocks.DIRT
              || block == Blocks.COARSE_DIRT)
          && level.getBlockState(pos.above()).isAir()) {
        return block == Blocks.COARSE_DIRT
            ? Blocks.DIRT.defaultBlockState()
            : Blocks.FARMLAND.defaultBlockState();
      }
    }

    return ability != ItemAbilities.FIRESTARTER_LIGHT
            || !CampfireBlock.canLight(state)
                && !CandleBlock.canLight(state)
                && !CandleCakeBlock.canLight(state)
        ? null
        : (BlockState) state.setValue(BlockStateProperties.LIT, true);
  }
}
