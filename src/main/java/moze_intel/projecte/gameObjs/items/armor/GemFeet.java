package moze_intel.projecte.gameObjs.items.armor;

import java.util.function.Consumer;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.registries.PEArmorMaterials;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class GemFeet extends GemArmorBase {
  private static final Vec3 VERTICAL_MOVEMENT = new Vec3(0.0, 0.1, 0.0);
  private static final boolean STEP_ASSIST_DEFAULT = false;
  private static final AttributeModifier SPEED =
      new AttributeModifier(PECore.rl("armor"), 1.0, Operation.ADD_MULTIPLIED_TOTAL);
  private static final AttributeModifier STEP =
      new AttributeModifier(PECore.rl("gem_step_assist"), 0.4, Operation.ADD_VALUE);

  public GemFeet(Properties props) {
    super(
        ArmorType.BOOTS,
        props.component(PEDataComponentTypes.STEP_ASSIST.get(), false),
        createDefaultModifiers());
  }

  private static ItemAttributeModifiers createDefaultModifiers() {
    return PEArmorMaterials.GEM_ARMOR
        .createAttributes(ArmorType.BOOTS)
        .withModifierAdded(Attributes.MOVEMENT_SPEED, SPEED, EquipmentSlotGroup.FEET);
  }

  private static ItemAttributeModifiers createStepAssistModifiers() {
    return createDefaultModifiers()
        .withModifierAdded(Attributes.STEP_HEIGHT, STEP, EquipmentSlotGroup.FEET);
  }

  public static void toggleStepAssist(ItemStack boots, Player player) {
    boolean newValue = !isStepAssist(boots);
    boots.set(PEDataComponentTypes.STEP_ASSIST.get(), newValue);
    boots.set(
        DataComponents.ATTRIBUTE_MODIFIERS,
        newValue ? createStepAssistModifiers() : createDefaultModifiers());
    PlayerHelper.sendOverlayMessage(player, getComponent(newValue));
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (isArmorSlot(slot) && entity instanceof Player player) {
      player.resetFallDistance();
    }
  }

  public void applyClientMovement(Player player, boolean jumpPressed, float forwardInput) {
    boolean flying = player.getAbilities().flying;
    if (!flying && jumpPressed) {
      player.addDeltaMovement(VERTICAL_MOVEMENT);
    }

    if (!player.onGround()) {
      Vec3 deltaMovement = player.getDeltaMovement();
      if (deltaMovement.y() <= 0.0) {
        player.setDeltaMovement(deltaMovement = deltaMovement.multiply(1.0, 0.9, 1.0));
      }

      if (!flying) {
        if (forwardInput < 0.0F) {
          player.setDeltaMovement(deltaMovement.multiply(0.9, 1.0, 0.9));
        } else if (forwardInput > 0.0F && deltaMovement.lengthSqr() < 3.0) {
          player.setDeltaMovement(deltaMovement.multiply(1.1, 1.0, 1.1));
        }
      }
    }
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(PELang.GEM_LORE_FEET.translate());
    tooltip.accept(
        PELang.STEP_ASSIST_PROMPT.translate(ClientKeyHelper.getKeyName(PEKeybind.BOOTS_TOGGLE)));
    tooltip.accept(getComponent(isStepAssist(stack)));
  }

  private static boolean isStepAssist(ItemStack stack) {
    return (Boolean) stack.getOrDefault(PEDataComponentTypes.STEP_ASSIST.get(), false);
  }

  private static Component getComponent(boolean enabled) {
    return enabled
        ? PELang.STEP_ASSIST.translate(ChatFormatting.GREEN, PELang.GEM_ENABLED)
        : PELang.STEP_ASSIST.translate(ChatFormatting.RED, PELang.GEM_DISABLED);
  }
}
