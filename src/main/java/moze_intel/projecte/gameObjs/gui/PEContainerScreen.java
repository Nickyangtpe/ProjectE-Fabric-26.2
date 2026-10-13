package moze_intel.projecte.gameObjs.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class PEContainerScreen<T extends AbstractContainerMenu>
    extends AbstractContainerScreen<T> {
  public boolean switchingToJEI;

  public PEContainerScreen(T container, Inventory invPlayer, Component title) {
    super(container, invPlayer, title);
  }

  public PEContainerScreen(
      T container, Inventory invPlayer, Component title, int imageWidth, int imageHeight) {
    super(container, invPlayer, title, imageWidth, imageHeight);
  }

  protected final void blit(
      GuiGraphicsExtractor graphics,
      Identifier texture,
      int x,
      int y,
      int u,
      int v,
      int width,
      int height) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
  }

  public void removed() {
    if (!this.switchingToJEI) {
      super.removed();
    }
  }

  public void init() {
    this.switchingToJEI = false;
    super.init();
  }
}
