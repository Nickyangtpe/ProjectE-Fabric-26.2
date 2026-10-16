package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.gameObjs.container.EternalDensityContainer;
import moze_intel.projecte.network.packets.to_server.UpdateGemModePKT;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GUIEternalDensity extends PEContainerScreen<EternalDensityContainer> {
  private static final Identifier texture = PECore.rl("textures/gui/eternal_density.png");

  public GUIEternalDensity(EternalDensityContainer container, Inventory inv, Component title) {
    super(container, inv, title, 180, 180);
  }

  @Override
  public void init() {
    super.init();
    this.addRenderableWidget(
        Button.builder(
                (((EternalDensityContainer) this.menu).isWhitelistMode()
                        ? PELang.WHITELIST
                        : PELang.BLACKLIST)
                    .translate(),
                b -> {
                  boolean isWhitelistMode =
                      !((EternalDensityContainer) this.menu).isWhitelistMode();
                  PacketDistributor.sendToServer(
                      new UpdateGemModePKT(
                          ((EternalDensityContainer) this.menu).hand, isWhitelistMode));
                  b.setMessage(
                      isWhitelistMode
                          ? PELang.WHITELIST.translate()
                          : PELang.BLACKLIST.translate());
                })
            .pos(this.leftPos + 62, this.topPos + 4)
            .size(52, 20)
            .build());
  }

  public void extractContents(
      @NotNull GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
    this.blit(
        graphics, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    super.extractContents(graphics, x, y, partialTicks);
  }

  protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {}
}
