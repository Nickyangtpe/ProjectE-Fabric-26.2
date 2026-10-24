package moze_intel.projecte.rendering;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Compact contextual controls for ProjectE items held by the local player. */
public final class ContextualItemHUD {
  private static final Identifier HUD_ID = PECore.rl("contextual_item_hud");

  private ContextualItemHUD() {}

  public static void register() {
    HudElementRegistry.attachElementAfter(
        VanillaHudElements.HOTBAR, HUD_ID, ContextualItemHUD::render);
  }

  private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;
    if (!ProjectEConfig.client.contextualHud.get()
        || player == null
        || minecraft.gui.hud.isHidden()
        || minecraft.gui.screen() != null) {
      return;
    }

    boolean showItemIcon = ProjectEConfig.client.contextualHudItemIcon.get();
    State state =
        getState(
            player,
            ProjectEConfig.client.contextualHudMode.get(),
            ProjectEConfig.client.contextualHudCharge.get(),
            ProjectEConfig.client.contextualHudKeyHints.get());
    if (state == null) {
      return;
    }

    Font font = minecraft.font;
    int contentWidth = Math.max(font.width(state.title()), font.width(state.status()));
    contentWidth = Math.max(contentWidth, font.width(state.hints()));
    int maximumWidth = Math.max(80, Math.min(230, graphics.guiWidth() - 12));
    int minimumWidth = Math.min(128, maximumWidth);
    int textOffset = showItemIcon ? 30 : 8;
    int width = Mth.clamp(contentWidth + textOffset + 6, minimumWidth, maximumWidth);
    int x = Math.max(6, graphics.guiWidth() - width - 8);
    int textX = x + textOffset;
    int textWidth = width - textOffset - 6;
    List<FormattedCharSequence> statusLines = font.split(state.status(), textWidth);
    List<FormattedCharSequence> hintLines = font.split(state.hints(), textWidth);
    int afterStatus = 17 + statusLines.size() * 9;
    int barY = afterStatus + 2;
    int hintY = state.maximumCharge() > 0 ? barY + 9 : afterStatus + 1;
    int height = hintY + hintLines.size() * 9 + 3;
    int hotbarLeft = graphics.guiWidth() / 2 - 94;
    int hotbarRight = graphics.guiWidth() / 2 + 94;
    int bottomMargin = x < hotbarRight && x + width > hotbarLeft ? 28 : 8;
    int y = Math.max(4, graphics.guiHeight() - height - bottomMargin);

    graphics.fillGradient(x, y, x + width, y + height, 0xDD171326, 0xD9080A12);
    graphics.fill(x, y, x + 3, y + height, state.accentColor());
    graphics.outline(x, y, width, height, 0xA06F63A8);
    if (showItemIcon) {
      graphics.item(state.stack(), x + 8, y + 7);
    }

    graphics.text(
        font,
        font.split(state.title(), textWidth).getFirst(),
        textX,
        y + 5,
        0xFFF5F2FF,
        true);
    for (int index = 0; index < statusLines.size(); index++) {
      graphics.text(
          font, statusLines.get(index), textX, y + 17 + index * 9, 0xFFD8D2EA, false);
    }
    if (state.maximumCharge() > 0) {
      drawChargeBar(
          graphics, textX, y + barY, textWidth, state.charge(), state.maximumCharge());
    }
    for (int index = 0; index < hintLines.size(); index++) {
      graphics.text(
          font, hintLines.get(index), textX, y + hintY + index * 9, 0xFF9D96B3, false);
    }
  }

  private static void drawChargeBar(
      GuiGraphicsExtractor graphics,
      int x,
      int y,
      int width,
      int charge,
      int maximumCharge) {
    graphics.fill(x, y, x + width, y + 5, 0xB0252035);
    int gap = maximumCharge > 8 ? 0 : 1;
    for (int index = 0; index < maximumCharge; index++) {
      int start = x + index * width / maximumCharge;
      int end = x + (index + 1) * width / maximumCharge - gap;
      int color =
          index < charge
              ? blend(0xFF8D4CFF, 0xFF35E4CA, index, maximumCharge)
              : 0xFF383248;
      graphics.fill(start, y, Math.max(start + 1, end), y + 5, color);
    }
  }

  private static int blend(int from, int to, int index, int count) {
    float progress = count <= 1 ? 1 : (float) index / (count - 1);
    int red =
        (from >> 16 & 0xFF)
            + Math.round(((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * progress);
    int green =
        (from >> 8 & 0xFF)
            + Math.round(((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * progress);
    int blue = (from & 0xFF) + Math.round(((to & 0xFF) - (from & 0xFF)) * progress);
    return 0xFF000000 | red << 16 | green << 8 | blue;
  }

  @Nullable
  private static State getState(
      Player player, boolean showMode, boolean showCharge, boolean showKeyHints) {
    for (InteractionHand hand : InteractionHand.values()) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.isEmpty()) {
        continue;
      }
      IItemCharge chargeItem = PECapabilities.CHARGE_ITEM_CAPABILITY.find(stack, null);
      IModeChanger<?> modeChanger =
          PECapabilities.MODE_CHANGER_ITEM_CAPABILITY.find(stack, null);
      IProjectileShooter projectile =
          PECapabilities.PROJECTILE_SHOOTER_ITEM_CAPABILITY.find(stack, null);
      IExtraFunction extra = PECapabilities.EXTRA_FUNCTION_ITEM_CAPABILITY.find(stack, null);
      if (chargeItem == null && modeChanger == null && projectile == null && extra == null) {
        continue;
      }

      int charge = chargeItem == null || !showCharge ? 0 : chargeItem.getCharge(stack);
      int maximumCharge =
          chargeItem == null || !showCharge ? 0 : chargeItem.getNumCharges(stack);
      Component status = createStatus(stack, modeChanger, showMode, charge, maximumCharge);
      Component hints =
          showKeyHints
              ? createHints(
                  modeChanger != null, chargeItem != null, projectile != null, extra != null)
              : Component.empty();
      int accent =
          modeChanger != null && Boolean.FALSE.equals(modeChanger.getMode(stack))
              ? 0xFFFFB347
              : 0xFF8D4CFF;
      return new State(stack, stack.getHoverName(), status, hints, charge, maximumCharge, accent);
    }
    return null;
  }

  private static Component createStatus(
      ItemStack stack,
      @Nullable IModeChanger<?> modeChanger,
      boolean showMode,
      int charge,
      int maximumCharge) {
    MutableComponent status = Component.empty();
    if (showMode && modeChanger != null) {
      status.append(PELang.CURRENT_MODE.translate(modeName(modeChanger.getMode(stack))));
    }
    if (maximumCharge > 0) {
      if (!status.getSiblings().isEmpty()) {
        status.append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY));
      }
      status.append(Component.translatable("charge.projecte.level", charge, maximumCharge));
    }
    return status.getString().isEmpty()
        ? Component.translatable("hud.projecte.ready").withStyle(ChatFormatting.AQUA)
        : status;
  }

  private static Component modeName(Object mode) {
    if (mode instanceof IHasTranslationKey.IHasEnumNameTranslationKey translated) {
      return translated.getTranslatedName();
    }
    if (mode instanceof Boolean active) {
      return (active ? PELang.GEM_ENABLED : PELang.GEM_DISABLED)
          .translateColored(active ? ChatFormatting.GREEN : ChatFormatting.GOLD);
    }
    return Component.literal(String.valueOf(mode));
  }

  private static Component createHints(
      boolean mode, boolean charge, boolean projectile, boolean extra) {
    List<Component> hints = new ArrayList<>(4);
    if (mode) {
      hints.add(
          Component.translatable(
              "hud.projecte.hint.mode", ClientKeyHelper.getKeyName(PEKeybind.MODE)));
    }
    if (charge) {
      hints.add(
          Component.translatable(
              "hud.projecte.hint.charge", ClientKeyHelper.getKeyName(PEKeybind.CHARGE)));
    }
    if (projectile) {
      hints.add(
          Component.translatable(
              "hud.projecte.hint.projectile",
              ClientKeyHelper.getKeyName(PEKeybind.FIRE_PROJECTILE)));
    }
    if (extra) {
      hints.add(
          Component.translatable(
              "hud.projecte.hint.extra",
              ClientKeyHelper.getKeyName(PEKeybind.EXTRA_FUNCTION)));
    }
    MutableComponent result = Component.empty();
    for (int index = 0; index < hints.size(); index++) {
      if (index > 0) {
        result.append(Component.literal("  "));
      }
      result.append(hints.get(index));
    }
    return result;
  }

  private record State(
      ItemStack stack,
      Component title,
      Component status,
      Component hints,
      int charge,
      int maximumCharge,
      int accentColor) {}
}
