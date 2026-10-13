package moze_intel.projecte.gameObjs.items.tools;

import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.event.ItemAttributeModifierEvent;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEHammer extends PETool implements IHasConditionalAttributes {
  public PEHammer(IMatterType matterType, int numCharges, Properties props) {
    super(
        matterType,
        PETags.Blocks.MINEABLE_WITH_PE_HAMMER,
        numCharges,
        props.attributes(createAttributes(matterType, 10.0F, -3.0F)));
  }

  @Override
  public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility toolAction) {
    return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(toolAction)
        || ToolHelper.DEFAULT_PE_HAMMER_ACTIONS.contains(toolAction);
  }

  public void hurtEnemy(
      @NotNull ItemStack stack, @NotNull LivingEntity damaged, @NotNull LivingEntity damager) {
    ToolHelper.attackWithCharge(stack, damaged, damager, 1.0F);
  }

  @Override
  public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
    return ToolHelper.canMatterMine(this.matterType, state.getBlock())
        ? 1200000.0F
        : super.getDestroySpeed(stack, state);
  }

  @Override
  public void adjustAttributes(ItemAttributeModifierEvent event) {
    ToolHelper.applyChargeAttributes(event);
  }

  @NotNull
  public InteractionResult useOn(UseOnContext context) {
    Player player = context.getPlayer();
    return (InteractionResult)
        (player == null
            ? InteractionResult.PASS
            : ToolHelper.digAOE(
                context.getLevel(),
                player,
                context.getHand(),
                context.getItemInHand(),
                context.getClickedPos(),
                context.getClickedFace(),
                true,
                0L));
  }
}
