package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.RelayMK1Container;
import moze_intel.projecte.gameObjs.container.RelayMK2Container;
import moze_intel.projecte.gameObjs.container.RelayMK3Container;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GUIRelay<CONTAINER extends RelayMK1Container> extends PEContainerScreen<CONTAINER> {
  private final Identifier texture;
  private final int emcX;
  private final int emcY;
  private final int vOffset;
  private final int emcBarShift;
  private final int shiftX;
  private final int shiftY;

  protected GUIRelay(
      CONTAINER container,
      Inventory invPlayer,
      Component title,
      Identifier texture,
      int emcX,
      int emcY,
      int vOffset,
      int emcBarShift,
      int shiftX,
      int shiftY,
      int imageWidth,
      int imageHeight) {
    super(container, invPlayer, title, imageWidth, imageHeight);
    this.texture = texture;
    this.emcX = emcX;
    this.emcY = emcY;
    this.vOffset = vOffset;
    this.emcBarShift = emcBarShift;
    this.shiftX = shiftX;
    this.shiftY = shiftY;
  }

  protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {
    graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
    graphics.text(
        this.font,
        EMCHelper.formatEmc(((RelayMK1Container) this.menu).emc.get()),
        this.emcX,
        this.emcY,
        -12566464,
        false);
  }

  public void extractContents(
      @NotNull GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
    this.blit(
        graphics, this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    int progress =
        (int)
            ((double) ((RelayMK1Container) this.menu).emc.get()
                / ((RelayMK1Container) this.menu).relay.getMaximumEmc()
                * 102.0);
    this.blit(
        graphics,
        this.texture,
        this.leftPos + this.emcBarShift,
        this.topPos + 6,
        30,
        this.vOffset,
        progress,
        10);
    progress = (int) (((RelayMK1Container) this.menu).getKleinChargeProgress() * 30.0);
    this.blit(
        graphics,
        this.texture,
        this.leftPos + 116 + this.shiftX,
        this.topPos + 67 + this.shiftY,
        0,
        this.vOffset,
        progress,
        10);
    progress = (int) (((RelayMK1Container) this.menu).getInputBurnProgress() * 30.0);
    this.blit(
        graphics,
        this.texture,
        this.leftPos + 64 + this.shiftX,
        this.topPos + 67 + this.shiftY,
        0,
        this.vOffset,
        progress,
        10);
    super.extractContents(graphics, x, y, partialTicks);
  }

  public static class GUIRelayMK1 extends GUIRelay<RelayMK1Container> {
    private static final Identifier MK1_TEXTURE = PECore.rl("textures/gui/relay1.png");

    public GUIRelayMK1(RelayMK1Container container, Inventory invPlayer, Component title) {
      super(container, invPlayer, title, MK1_TEXTURE, 88, 24, 177, 64, 0, 0, 175, 176);
      this.titleLabelX = 10;
    }
  }

  public static class GUIRelayMK2 extends GUIRelay<RelayMK2Container> {
    private static final Identifier MK2_TEXTURE = PECore.rl("textures/gui/relay2.png");

    public GUIRelayMK2(RelayMK2Container container, Inventory invPlayer, Component title) {
      super(container, invPlayer, title, MK2_TEXTURE, 107, 25, 183, 86, 17, 1, 193, 182);
      this.titleLabelX = 28;
    }
  }

  public static class GUIRelayMK3 extends GUIRelay<RelayMK3Container> {
    private static final Identifier MK3_TEXTURE = PECore.rl("textures/gui/relay3.png");

    public GUIRelayMK3(RelayMK3Container container, Inventory invPlayer, Component title) {
      super(container, invPlayer, title, MK3_TEXTURE, 125, 39, 195, 105, 37, 15, 212, 194);
      this.titleLabelX = 38;
    }
  }
}
