package moze_intel.projecte.gameObjs.items.tools;

import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.ItemAbilityProvider;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEShovel extends ShovelItem implements IItemCharge, IBarHelper, ItemAbilityProvider {
  private final IMatterType matterType;
  private final int numCharges;

  public PEShovel(IMatterType matterType, int numCharges, Properties props) {
    super(
        PETool.toolMaterial(matterType),
        2.0F,
        -3.0F,
        props
            .component(PEDataComponentTypes.CHARGE.get(), 0)
            .component(PEDataComponentTypes.STORED_EMC.get(), 0L));
    this.matterType = matterType;
    this.numCharges = numCharges;
  }

  @Override
  public boolean canPerformAction(ItemStack stack, ItemAbility action) {
    return ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(action);
  }

  public boolean isBarVisible(@NotNull ItemStack stack) {
    return true;
  }

  @Override
  public float getWidthForBar(ItemStack stack) {
    return 1.0F - this.getChargePercent(stack);
  }

  public int getBarWidth(@NotNull ItemStack stack) {
    return this.getScaledBarWidth(stack);
  }

  public int getBarColor(@NotNull ItemStack stack) {
    return this.getColorForBar(stack);
  }

  public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
    return ToolHelper.getDestroySpeed(
        super.getDestroySpeed(stack, state), this.matterType, this.getCharge(stack));
  }

  @Override
  public int getNumCharges(@NotNull ItemStack stack) {
    return this.numCharges;
  }

  @NotNull
  public InteractionResult useOn(UseOnContext context) {
    Player player = context.getPlayer();
    if (player == null) {
      return InteractionResult.PASS;
    }

    Level level = context.getLevel();
    BlockPos pos = context.getClickedPos();
    BlockState blockState = level.getBlockState(pos);
    return ToolHelper.performActions(
        context,
        blockState,
        ToolHelper.flattenAOE(context, blockState, 0L),
        ToolHelper::dowseCampfire,
        (ctx, state) ->
            (InteractionResult)
                (state.is(PETags.Blocks.VEIN_SHOVEL)
                    ? ToolHelper.tryVeinMine(
                        ctx.getPlayer(),
                        ctx.getItemInHand(),
                        ctx.getClickedPos(),
                        ctx.getClickedFace())
                    : InteractionResult.PASS),
        (ctx, state) ->
            ToolHelper.digAOE(
                ctx.getLevel(),
                ctx.getPlayer(),
                ctx.getHand(),
                ctx.getItemInHand(),
                ctx.getClickedPos(),
                ctx.getClickedFace(),
                false,
                0L));
  }
}
