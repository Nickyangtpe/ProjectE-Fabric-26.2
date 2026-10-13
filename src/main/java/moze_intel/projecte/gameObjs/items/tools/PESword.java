package moze_intel.projecte.gameObjs.items.tools;

import java.util.List;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.ItemAbilityProvider;
import moze_intel.projecte.compat.common.ToolRuleHelper;
import moze_intel.projecte.compat.event.ItemAttributeModifierEvent;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class PESword extends Item
    implements IExtraFunction,
        IItemCharge,
        IBarHelper,
        IHasConditionalAttributes,
        ItemAbilityProvider {
  private final IMatterType matterType;
  private final int numCharges;

  public PESword(IMatterType matterType, int numCharges, int damage, Properties props) {
    super(
        props
            .attributes(PETool.createAttributes(matterType, damage, -2.4F))
            .component(PEDataComponentTypes.CHARGE.get(), 0)
            .component(PEDataComponentTypes.STORED_EMC.get(), 0L)
            .component(
                DataComponents.TOOL,
                new Tool(
                    List.of(
                        Rule.deniesDrops(
                            ToolRuleHelper.fromTag(matterType.getIncorrectBlocksForDrops())),
                        Rule.minesAndDrops(
                            ToolRuleHelper.fromTag(PETags.Blocks.MINEABLE_WITH_PE_SWORD),
                            matterType.getSpeed()),
                        Rule.overrideSpeed(
                            ToolRuleHelper.fromTag(BlockTags.SWORD_EFFICIENT), 1.5F)),
                    1.0F,
                    2,
                    true))
            .component(DataComponents.WEAPON, new Weapon(1)));
    this.matterType = matterType;
    this.numCharges = numCharges;
  }

  @Override
  public boolean canPerformAction(ItemStack stack, ItemAbility action) {
    return ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(action);
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

  public void hurtEnemy(
      @NotNull ItemStack stack, @NotNull LivingEntity damaged, @NotNull LivingEntity damager) {
    ToolHelper.attackWithCharge(stack, damaged, damager, 1.0F);
  }

  @NotNull
  public AABB getSweepHitBox(
      @NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target) {
    int charge = this.getCharge(stack);
    return target.getBoundingBox().inflate(charge, charge / 4.0, charge);
  }

  @Override
  public boolean doExtraFunction(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    if (player.getAttackStrengthScale(0.0F) == 1.0F) {
      ToolHelper.attackAOE(
          stack,
          player,
          this.slayAll(stack),
          (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE),
          0L,
          hand);
      PlayerHelper.resetCooldown(player);
      return true;
    } else {
      return false;
    }
  }

  protected boolean slayAll(@NotNull ItemStack stack) {
    return false;
  }

  @Override
  public void adjustAttributes(ItemAttributeModifierEvent event) {
    ToolHelper.applyChargeAttributes(event);
  }
}
