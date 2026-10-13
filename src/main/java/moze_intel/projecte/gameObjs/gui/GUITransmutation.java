package moze_intel.projecte.gameObjs.gui;

import java.math.BigInteger;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GUITransmutation extends PEContainerScreen<TransmutationContainer> {
  private static final BigInteger MAX_EXACT_TRANSMUTATION_DISPLAY =
      BigInteger.valueOf(1000000000000L);
  private static final Identifier texture = PECore.rl("textures/gui/transmute.png");
  private final TransmutationInventory inv;
  private EditBox textBoxFilter;
  private Button previous;
  private Button next;

  public GUITransmutation(TransmutationContainer container, Inventory invPlayer, Component title) {
    super(container, invPlayer, title, 228, 196);
    this.inv = container.transmutationInventory;
    this.titleLabelX = 6;
    this.titleLabelY = 8;
  }

  @Override
  public void init() {
    super.init();
    this.textBoxFilter =
        (EditBox)
            this.addRenderableWidget(
                new EditBox(
                    this.font, this.leftPos + 79, this.topPos + 8, 55, 14, Component.empty()));
    this.textBoxFilter.setMaxLength(128);
    this.textBoxFilter.setHint(PELang.TRANSMUTATION_SEARCH_HINT.translate());
    this.textBoxFilter.setTooltip(Tooltip.create(PELang.TRANSMUTATION_SEARCH_TOOLTIP.translate()));
    this.textBoxFilter.setResponder(this.inv::updateFilter);
    this.previous =
        (Button)
            this.addRenderableWidget(
                Button.builder(Component.literal("<"), b -> this.inv.previousPage())
                    .pos(this.leftPos + 125, this.topPos + 100)
                    .size(14, 14)
                    .build());
    this.next =
        (Button)
            this.addRenderableWidget(
                Button.builder(Component.literal(">"), b -> this.inv.nextPage())
                    .pos(this.leftPos + 193, this.topPos + 100)
                    .size(14, 14)
                    .build());
    this.updateButtons();
  }

  public void resize(int width, int height) {
    String filter = this.textBoxFilter == null ? "" : this.textBoxFilter.getValue();
    super.resize(width, height);
    if (this.textBoxFilter != null) {
      this.textBoxFilter.setValue(filter);
    }
  }

  protected void containerTick() {
    super.containerTick();
    this.updateButtons();
  }

  private void updateButtons() {
    if (this.previous != null) {
      this.previous.active = this.inv.hasPreviousPage();
    }

    if (this.next != null) {
      this.next.active = this.inv.hasNextPage();
    }
  }

  public void extractContents(
      @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    this.blit(
        graphics, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    super.extractContents(graphics, mouseX, mouseY, partialTicks);
  }

  protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {
    graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
    graphics.text(
        this.font, PELang.EMC_TOOLTIP.translate(""), 6, this.imageHeight - 104, -12566464, false);
    Component emc = TransmutationEMCFormatter.formatEMC(this.inv.getAvailableEmc());
    graphics.text(this.font, emc, 6, this.imageHeight - 94, -12566464, false);
    if (this.inv.learnFlag > 0) {
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_1.translate(), 98, 30, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_2.translate(), 99, 38, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_3.translate(), 100, 46, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_4.translate(), 101, 54, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_5.translate(), 102, 62, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_6.translate(), 103, 70, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_7.translate(), 104, 78, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_LEARNED_8.translate(), 107, 86, -12566464, false);
      this.inv.learnFlag--;
    }

    if (this.inv.unlearnFlag > 0) {
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_1.translate(), 97, 22, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_2.translate(), 98, 30, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_3.translate(), 99, 38, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_4.translate(), 100, 46, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_5.translate(), 101, 54, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_6.translate(), 102, 62, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_7.translate(), 103, 70, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_8.translate(), 104, 78, -12566464, false);
      graphics.text(
          this.font, PELang.TRANSMUTATION_UNLEARNED_9.translate(), 107, 86, -12566464, false);
      this.inv.unlearnFlag--;
    }
  }

  public boolean keyPressed(KeyEvent event) {
    if (this.textBoxFilter.isFocused()) {
      if (event.key() == 256) {
        this.textBoxFilter.setFocused(false);
        return true;
      } else {
        return this.textBoxFilter.keyPressed(event);
      }
    } else {
      return super.keyPressed(event);
    }
  }

  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (this.textBoxFilter.isMouseOver(event.x(), event.y())) {
      if (event.button() == 1) {
        this.textBoxFilter.setValue("");
      }
    } else if (this.textBoxFilter.isFocused()
        && (this.hoveredSlot == null
            || !this.hoveredSlot.hasItem()
                && ((TransmutationContainer) this.menu).getCarried().isEmpty())) {
      this.textBoxFilter.setFocused(false);
    }

    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public void removed() {
    super.removed();
    this.inv.learnFlag = 0;
    this.inv.unlearnFlag = 0;
  }

  protected void extractTooltip(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    BigInteger emcAmount = this.inv.getAvailableEmc();
    if (emcAmount.compareTo(MAX_EXACT_TRANSMUTATION_DISPLAY) < 0) {
      super.extractTooltip(graphics, mouseX, mouseY);
    } else {
      int emcLeft = this.leftPos;
      int emcRight = emcLeft + 82;
      int emcTop = 95 + this.topPos;
      int emcBottom = emcTop + 15;
      if (mouseX > emcLeft && mouseX < emcRight && mouseY > emcTop && mouseY < emcBottom) {
        graphics.setTooltipForNextFrame(
            this.font,
            PELang.EMC_TOOLTIP.translate(EMCHelper.formatEmc(emcAmount)),
            mouseX,
            mouseY);
      } else {
        super.extractTooltip(graphics, mouseX, mouseY);
      }
    }
  }
}
