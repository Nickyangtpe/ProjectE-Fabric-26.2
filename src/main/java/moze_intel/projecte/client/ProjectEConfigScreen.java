package moze_intel.projecte.client;

import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.config.value.CachedBooleanValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ProjectEConfigScreen extends Screen {
  private final Screen parent;

  public ProjectEConfigScreen(Screen parent) {
    super(Component.translatable("configuration.projecte.client.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int centerX = this.width / 2;
    int columnGap = 8;
    int optionWidth = Math.max(80, Math.min(190, (this.width - 28 - columnGap) / 2));
    int leftX = centerX - optionWidth - columnGap / 2;
    int rightX = centerX + columnGap / 2;
    int startY = Math.max(30, this.height / 2 - 70);

    this.addBooleanOption(
        leftX,
        startY,
        optionWidth,
        "configuration.projecte.client.context_hud",
        ProjectEConfig.client.contextualHud);
    this.addBooleanOption(
        rightX,
        startY,
        optionWidth,
        "configuration.projecte.client.context_hud.icon",
        ProjectEConfig.client.contextualHudItemIcon);
    this.addBooleanOption(
        leftX,
        startY + 22,
        optionWidth,
        "configuration.projecte.client.context_hud.mode",
        ProjectEConfig.client.contextualHudMode);
    this.addBooleanOption(
        rightX,
        startY + 22,
        optionWidth,
        "configuration.projecte.client.context_hud.charge",
        ProjectEConfig.client.contextualHudCharge);
    this.addBooleanOption(
        leftX,
        startY + 44,
        optionWidth,
        "configuration.projecte.client.context_hud.hints",
        ProjectEConfig.client.contextualHudKeyHints);
    this.addBooleanOption(
        rightX,
        startY + 44,
        optionWidth,
        "configuration.projecte.client.philo_overlay",
        ProjectEConfig.client.pulsatingOverlay);
    this.addBooleanOption(
        leftX,
        startY + 66,
        optionWidth,
        "configuration.projecte.client.tooltips.emc",
        ProjectEConfig.client.emcToolTips);
    this.addBooleanOption(
        rightX,
        startY + 66,
        optionWidth,
        "configuration.projecte.client.tooltips.emc.shift",
        ProjectEConfig.client.shiftEmcToolTips);
    this.addBooleanOption(
        leftX,
        startY + 88,
        optionWidth,
        "configuration.projecte.client.tooltips.learned.shift",
        ProjectEConfig.client.shiftLearnedToolTips);
    this.addBooleanOption(
        rightX,
        startY + 88,
        optionWidth,
        "configuration.projecte.client.tooltips.pedestal",
        ProjectEConfig.client.pedestalToolTips);
    this.addBooleanOption(
        leftX,
        startY + 110,
        optionWidth,
        "configuration.projecte.client.tooltips.stats",
        ProjectEConfig.client.statToolTips);
    this.addBooleanOption(
        rightX,
        startY + 110,
        optionWidth,
        "configuration.projecte.client.tooltips.tags",
        ProjectEConfig.client.tagToolTips);

    int doneWidth = Math.min(200, this.width - 20);
    int doneY = Math.min(this.height - 28, startY + 138);
    this.addRenderableWidget(
        Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(centerX - doneWidth / 2, doneY, doneWidth, 20)
            .build());
  }

  private void addBooleanOption(
      int x, int y, int width, String translationKey, CachedBooleanValue configValue) {
    Component label = Component.translatable(translationKey);
    Component description = Component.translatable(translationKey + ".tooltip");
    this.addRenderableWidget(
        CycleButton.onOffBuilder(configValue.get())
            .withTooltip(value -> Tooltip.create(description))
            .create(
                x,
                y,
                width,
                20,
                label,
                (button, enabled) -> {
                  configValue.set(enabled);
                  ProjectEConfig.client.save();
                }));
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
  }

  @Override
  public void onClose() {
    this.minecraft.gui.setScreen(this.parent);
  }
}
