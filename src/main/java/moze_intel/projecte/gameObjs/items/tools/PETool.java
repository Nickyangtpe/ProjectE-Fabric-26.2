package moze_intel.projecte.gameObjs.items.tools;

import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.ItemAbilityProvider;
import moze_intel.projecte.compat.common.ToolRuleHelper;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class PETool extends Item implements IItemCharge, IBarHelper, ItemAbilityProvider {
  protected final IMatterType matterType;
  private final int numCharges;

  public PETool(IMatterType matterType, TagKey<Block> blocks, int numCharges, Properties props) {
    super(
        props
            .component(
                DataComponents.TOOL,
                new Tool(
                    List.of(
                        Rule.deniesDrops(
                            ToolRuleHelper.fromTag(matterType.getIncorrectBlocksForDrops())),
                        Rule.minesAndDrops(ToolRuleHelper.fromTag(blocks), matterType.getSpeed())),
                    1.0F,
                    2,
                    true))
            .component(PEDataComponentTypes.CHARGE.get(), 0)
            .component(PEDataComponentTypes.STORED_EMC.get(), 0L));
    this.matterType = matterType;
    this.numCharges = numCharges;
  }

  protected static ToolMaterial toolMaterial(IMatterType matterType) {
    TagKey<Item> noRepair = TagKey.create(Registries.ITEM, PECore.rl("never_repairable"));
    return new ToolMaterial(
        matterType.getIncorrectBlocksForDrops(),
        matterType.getUses(),
        matterType.getSpeed(),
        matterType.getAttackDamageBonus(),
        matterType.getEnchantmentValue(),
        noRepair);
  }

  protected static ItemAttributeModifiers createAttributes(
      IMatterType material, float attackDamage, float attackSpeed) {
    return ItemAttributeModifiers.builder()
        .add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(
                Item.BASE_ATTACK_DAMAGE_ID,
                material.getAttackDamageBonus() + attackDamage,
                Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .build();
  }

  @Override
  public boolean canPerformAction(@NotNull ItemStack stack, ItemAbility action) {
    return false;
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

  protected float getShortCutDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
    return super.getDestroySpeed(stack, state);
  }
}
