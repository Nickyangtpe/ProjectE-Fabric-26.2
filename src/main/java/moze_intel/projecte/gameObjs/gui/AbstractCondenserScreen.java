package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.container.CondenserMK2Container;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCondenserScreen<T extends CondenserContainer>
    extends PEContainerScreen<T> {
  public AbstractCondenserScreen(T condenser, Inventory playerInventory, Component title) {
    super(condenser, playerInventory, title, 255, 233);
  }

  protected abstract Identifier getTexture();

  public void extractContents(
      @NotNull GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
    this.blit(
        graphics,
        this.getTexture(),
        this.leftPos,
        this.topPos,
        0,
        0,
        this.imageWidth,
        this.imageHeight);
    int progress = ((CondenserContainer) this.menu).getProgressScaled();
    this.blit(
        graphics, this.getTexture(), this.leftPos + 33, this.topPos + 10, 0, 235, progress, 10);
    super.extractContents(graphics, x, y, partialTicks);
  }

  protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {
    long toDisplay =
        Math.min(
            ((CondenserContainer) this.menu).displayEmc.get(),
            ((CondenserContainer) this.menu).requiredEmc.get());
    Component emc = TransmutationEMCFormatter.formatEMC(toDisplay);
    graphics.text(this.font, emc, 140, 10, -12566464, false);
  }

  protected void extractTooltip(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    long toDisplay =
        Math.min(
            ((CondenserContainer) this.menu).displayEmc.get(),
            ((CondenserContainer) this.menu).requiredEmc.get());
    if (toDisplay < 1.0E12) {
      super.extractTooltip(graphics, mouseX, mouseY);
    } else {
      int emcLeft = 140 + this.leftPos;
      int emcRight = emcLeft + 110;
      int emcTop = 6 + this.topPos;
      int emcBottom = emcTop + 15;
      if (mouseX > emcLeft && mouseX < emcRight && mouseY > emcTop && mouseY < emcBottom) {
        graphics.setTooltipForNextFrame(
            this.font,
            PELang.EMC_TOOLTIP.translate(EMCHelper.formatEmc(toDisplay)),
            mouseX,
            mouseY);
      } else {
        super.extractTooltip(graphics, mouseX, mouseY);
      }
    }
  }

  public static class MK1 extends AbstractCondenserScreen<CondenserContainer> {
    public MK1(CondenserContainer condenser, Inventory playerInventory, Component title) {
      super(condenser, playerInventory, title);
    }

    @Override
    protected Identifier getTexture() {
      return PECore.rl("textures/gui/condenser.png");
    }
  }

  public static class MK2 extends AbstractCondenserScreen<CondenserMK2Container> {
    public MK2(CondenserMK2Container condenser, Inventory playerInventory, Component title) {
      super(condenser, playerInventory, title);
    }

    @Override
    protected Identifier getTexture() {
      return PECore.rl("textures/gui/condenser_mk2.png");
    }
  }
}
