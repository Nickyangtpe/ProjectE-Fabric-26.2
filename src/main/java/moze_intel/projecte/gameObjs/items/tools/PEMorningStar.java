package moze_intel.projecte.gameObjs.items.tools;

import java.util.function.Consumer;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.Tags;
import moze_intel.projecte.compat.event.ItemAttributeModifierEvent;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import moze_intel.projecte.gameObjs.items.IItemMode;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEMorningStar extends PETool
    implements IItemMode<PEPickaxe.PickaxeMode>, IHasConditionalAttributes {
  public PEMorningStar(IMatterType matterType, int numCharges, Properties props) {
    super(
        matterType,
        PETags.Blocks.MINEABLE_WITH_PE_MORNING_STAR,
        numCharges,
        props
            .attributes(createAttributes(matterType, 16.0F, -3.0F))
            .component(PEDataComponentTypes.PICKAXE_MODE.get(), PEPickaxe.PickaxeMode.STANDARD));
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(this.getToolTip(stack));
  }

  @Override
  public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility toolAction) {
    return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(toolAction)
        || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(toolAction)
        || ToolHelper.DEFAULT_PE_HAMMER_ACTIONS.contains(toolAction)
        || ToolHelper.DEFAULT_PE_MORNING_STAR_ACTIONS.contains(toolAction);
  }

  public void hurtEnemy(
      @NotNull ItemStack stack, @NotNull LivingEntity damaged, @NotNull LivingEntity damager) {
    ToolHelper.attackWithCharge(stack, damaged, damager, 1.0F);
  }

  public boolean mineBlock(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockState state,
      @NotNull BlockPos pos,
      @NotNull LivingEntity living) {
    return true;
  }

  @NotNull
  public InteractionResult useOn(UseOnContext context) {
    Player player = context.getPlayer();
    if (player == null) {
      return InteractionResult.PASS;
    }

    Level level = context.getLevel();
    BlockState blockState = level.getBlockState(context.getClickedPos());
    return ToolHelper.performActions(
        context,
        blockState,
        ToolHelper.flattenAOE(context, blockState, 0L),
        ToolHelper::dowseCampfire,
        (ctx, state) ->
            (InteractionResult)
                (state.is(PETags.Blocks.VEIN_SHOVEL)
                    ? (ProjectEConfig.server.items.pickaxeAoeVeinMining.get()
                        ? ToolHelper.digAOE(
                            ctx.getLevel(),
                            ctx.getPlayer(),
                            ctx.getHand(),
                            ctx.getItemInHand(),
                            ctx.getClickedPos(),
                            ctx.getClickedFace(),
                            false,
                            0L)
                        : ToolHelper.tryVeinMine(
                            ctx.getPlayer(),
                            ctx.getItemInHand(),
                            ctx.getClickedPos(),
                            ctx.getClickedFace()))
                    : InteractionResult.PASS),
        (ctx, state) ->
            (InteractionResult)
                (state.is(Tags.Blocks.ORES)
                        && !ProjectEConfig.server.items.pickaxeAoeVeinMining.get()
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
                !(state.getBlock() instanceof GrassBlock)
                    && !state.is(BlockTags.SAND)
                    && !state.is(BlockTags.DIRT),
                0L));
  }

  @NotNull
  public InteractionResult use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    return (InteractionResult)
        (ProjectEConfig.server.items.pickaxeAoeVeinMining.get()
            ? ItemHelper.actionResultFromType(ToolHelper.mineOreVeinsInAOE(player, hand), stack)
            : InteractionResult.PASS);
  }

  @Override
  public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
    return ToolHelper.canMatterMine(this.matterType, state.getBlock())
        ? 1200000.0F
        : super.getDestroySpeed(stack, state) + 48.0F;
  }

  @Override
  public void adjustAttributes(ItemAttributeModifierEvent event) {
    ToolHelper.applyChargeAttributes(event);
  }

  @Override
  public DataComponentType<PEPickaxe.PickaxeMode> getDataComponentType() {
    return PEDataComponentTypes.PICKAXE_MODE.get();
  }

  public PEPickaxe.PickaxeMode getDefaultMode() {
    return PEPickaxe.PickaxeMode.STANDARD;
  }
}
