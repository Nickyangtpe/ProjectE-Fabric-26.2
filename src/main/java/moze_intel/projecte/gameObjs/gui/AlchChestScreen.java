package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.AlchChestContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AlchChestScreen extends PEContainerScreen<AlchChestContainer> {
  private static final Identifier texture = PECore.rl("textures/gui/alchchest.png");

  public AlchChestScreen(AlchChestContainer container, Inventory invPlayer, Component title) {
    super(container, invPlayer, title, 255, 230);
  }

  public void extractContents(
      @NotNull GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
    this.blit(
        graphics, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    super.extractContents(graphics, x, y, partialTicks);
  }

  protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {}
}
