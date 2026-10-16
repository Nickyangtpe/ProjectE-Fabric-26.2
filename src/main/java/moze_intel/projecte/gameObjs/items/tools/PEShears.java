package moze_intel.projecte.gameObjs.items.tools;

import java.util.List;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.ItemAbilityProvider;
import moze_intel.projecte.compat.common.ToolRuleHelper;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEShears extends ShearsItem implements IItemCharge, IBarHelper, ItemAbilityProvider {
  private final IMatterType matterType;
  private final int numCharges;

  public PEShears(IMatterType matterType, int numCharges, Properties props) {
    super(
        props
            .component(PEDataComponentTypes.CHARGE.get(), 0)
            .component(PEDataComponentTypes.STORED_EMC.get(), 0L)
            .component(
                DataComponents.TOOL,
                new Tool(
                    List.of(
                        Rule.minesAndDrops(
                            ToolRuleHelper.fromTag(PETags.Blocks.MINEABLE_WITH_PE_SHEARS),
                            matterType.getSpeed()),
                        Rule.overrideSpeed(ToolRuleHelper.fromTag(BlockTags.LEAVES), 15.0F),
                        Rule.overrideSpeed(ToolRuleHelper.fromTag(BlockTags.WOOL), 5.0F),
                        Rule.overrideSpeed(
                            ToolRuleHelper.direct(Blocks.VINE, Blocks.GLOW_LICHEN), 2.0F)),
                    1.0F,
                    1,
                    true)));
    this.matterType = matterType;
    this.numCharges = numCharges;
  }

  @Override
  public boolean canPerformAction(ItemStack stack, ItemAbility action) {
    return ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(action);
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
  public InteractionResult use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    return ItemHelper.actionResultFromType(
        ToolHelper.shearEntityAOE(player, hand, 0L), player.getItemInHand(hand));
  }

  public InteractionResult interactLivingEntity(
      @NotNull ItemStack stack,
      @NotNull Player player,
      @NotNull LivingEntity entity,
      @NotNull InteractionHand hand) {
    return ToolHelper.shearEntity(player, stack, entity);
  }

  @NotNull
  public InteractionResult useOn(UseOnContext context) {
    Player player = context.getPlayer();
    if (player != null) {
      Level level = context.getLevel();
      BlockState state = level.getBlockState(context.getClickedPos());
      if (state.is(BlockTags.LEAVES)) {
        ToolHelper.clearTagAOE(
            level, player, context.getHand(), context.getItemInHand(), 0L, BlockTags.LEAVES);
      }
    }

    return InteractionResult.PASS;
  }
}
