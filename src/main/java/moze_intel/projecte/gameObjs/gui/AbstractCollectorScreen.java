package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.CollectorMK1Container;
import moze_intel.projecte.gameObjs.container.CollectorMK2Container;
import moze_intel.projecte.gameObjs.container.CollectorMK3Container;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCollectorScreen<T extends CollectorMK1Container>
    extends PEContainerScreen<T> {
  public AbstractCollectorScreen(T container, Inventory invPlayer, Component title) {
    super(container, invPlayer, title);
  }

  protected AbstractCollectorScreen(
      T container, Inventory invPlayer, Component title, int imageWidth, int imageHeight) {
    super(container, invPlayer, title, imageWidth, imageHeight);
  }

  protected abstract Identifier getTexture();

  protected int getBonusXShift() {
    return 0;
  }

  protected int getTextureBonusXShift() {
    return 0;
  }

  protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {
    graphics.text(
        this.font,
        Long.toString(((CollectorMK1Container) this.menu).emc.get()),
        60 + this.getBonusXShift(),
        32,
        -12566464,
        false);
    long kleinCharge = ((CollectorMK1Container) this.menu).kleinEmc.get();
    if (kleinCharge > 0L) {
      graphics.text(
          this.font,
          EMCHelper.formatEmc(kleinCharge),
          60 + this.getBonusXShift(),
          44,
          -12566464,
          false);
    }
  }

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
    int progress = (int) (((CollectorMK1Container) this.menu).sunLevel.get() * 12.0 / 16.0);
    this.blit(
        graphics,
        this.getTexture(),
        this.leftPos + 126 + this.getBonusXShift(),
        this.topPos + 49 - progress,
        177 + this.getTextureBonusXShift(),
        13 - progress,
        12,
        progress);
    this.blit(
        graphics,
        this.getTexture(),
        this.leftPos + 64 + this.getBonusXShift(),
        this.topPos + 18,
        0,
        166,
        (int)
            ((double) ((CollectorMK1Container) this.menu).emc.get()
                / ((CollectorMK1Container) this.menu).collector.getMaximumEmc()
                * 48.0),
        10);
    progress = (int) (((CollectorMK1Container) this.menu).getKleinChargeProgress() * 48.0);
    this.blit(
        graphics,
        this.getTexture(),
        this.leftPos + 64 + this.getBonusXShift(),
        this.topPos + 58,
        0,
        166,
        progress,
        10);
    progress = (int) (((CollectorMK1Container) this.menu).getFuelProgress() * 24.0);
    this.blit(
        graphics,
        this.getTexture(),
        this.leftPos + 138 + this.getBonusXShift(),
        this.topPos + 55 - progress,
        176 + this.getTextureBonusXShift(),
        38 - progress,
        10,
        progress + 1);
    super.extractContents(graphics, x, y, partialTicks);
  }

  public static class MK1 extends AbstractCollectorScreen<CollectorMK1Container> {
    public MK1(CollectorMK1Container container, Inventory invPlayer, Component title) {
      super(container, invPlayer, title);
    }

    @Override
    protected Identifier getTexture() {
      return PECore.rl("textures/gui/collector1.png");
    }
  }

  public static class MK2 extends AbstractCollectorScreen<CollectorMK2Container> {
    public MK2(CollectorMK2Container container, Inventory invPlayer, Component title) {
      super(container, invPlayer, title, 200, 165);
    }

    @Override
    protected Identifier getTexture() {
      return PECore.rl("textures/gui/collector2.png");
    }

    @Override
    protected int getBonusXShift() {
      return 16;
    }

    @Override
    protected int getTextureBonusXShift() {
      return 25;
    }
  }

  public static class MK3 extends AbstractCollectorScreen<CollectorMK3Container> {
    public MK3(CollectorMK3Container container, Inventory invPlayer, Component title) {
      super(container, invPlayer, title, 218, 165);
    }

    @Override
    protected Identifier getTexture() {
      return PECore.rl("textures/gui/collector3.png");
    }

    @Override
    protected int getBonusXShift() {
      return 34;
    }

    @Override
    protected int getTextureBonusXShift() {
      return 43;
    }
  }
}
