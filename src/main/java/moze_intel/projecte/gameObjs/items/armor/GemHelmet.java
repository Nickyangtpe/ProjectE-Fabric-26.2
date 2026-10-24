package moze_intel.projecte.gameObjs.items.armor;

import java.util.function.Consumer;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.LightningHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;

public class GemHelmet extends GemArmorBase {
  private static final boolean NIGHT_VISION_DEFAULT = false;

  public GemHelmet(Properties props) {
    super(ArmorType.HELMET, props.component(PEDataComponentTypes.NIGHT_VISION.get(), false));
  }

  public static void toggleNightVision(ItemStack helm, Player player) {
    boolean oldValue = hasNightVision(helm);
    helm.set(PEDataComponentTypes.NIGHT_VISION.get(), !oldValue);
    PlayerHelper.sendOverlayMessage(player, getComponent(!oldValue));
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(PELang.GEM_LORE_HELM.translate());
    tooltip.accept(
        PELang.NIGHT_VISION_PROMPT.translate(ClientKeyHelper.getKeyName(PEKeybind.HELMET_TOGGLE)));
    tooltip.accept(getComponent(hasNightVision(stack)));
  }

  private static boolean hasNightVision(ItemStack stack) {
    return (Boolean) stack.getOrDefault(PEDataComponentTypes.NIGHT_VISION.get(), false);
  }

  private static Component getComponent(boolean nightVision) {
    return nightVision
        ? PELang.NIGHT_VISION.translate(ChatFormatting.GREEN, PELang.GEM_ENABLED)
        : PELang.NIGHT_VISION.translate(ChatFormatting.RED, PELang.GEM_DISABLED);
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (isArmorSlot(slot) && entity instanceof Player player) {
      if (PlayerHelper.checkHealCooldown(player)) {
        player.heal(2.0F);
      }

      if (hasNightVision(stack)) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
      } else {
        player.removeEffect(MobEffects.NIGHT_VISION);
      }
    }
  }

  public static void doZap(Player player) {
    if (ProjectEConfig.server.difficulty.offensiveAbilities.get()) {
      BlockHitResult strikeResult = PlayerHelper.getBlockLookingAt(player, 120.0);
      if (strikeResult.getType() != Type.MISS) {
        BlockPos strikePos = strikeResult.getBlockPos();
        ServerLevel level = (ServerLevel) player.level();
        LightningBolt lightning = LightningHelper.create(level);
        if (lightning != null) {
          lightning.setPos(Vec3.atCenterOf(strikePos));
          lightning.setCause((ServerPlayer) player);
          level.addFreshEntity(lightning);
        }
      }
    }
  }
}
