package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.container.DMFurnaceContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GUIDMFurnace<CONTAINER extends DMFurnaceContainer>
    extends PEContainerScreen<CONTAINER> {
  private static final Identifier LIT_PROGRESS_SPRITE =
      Identifier.withDefaultNamespace("container/furnace/lit_progress");
  private static final int LIT_SIZE = 14;
  private static final Identifier DM_FURNACE = PECore.rl("textures/gui/dmfurnace.png");
  private final DMFurnaceBlockEntity furnace;
  protected final Identifier texture;

  public GUIDMFurnace(CONTAINER container, Inventory invPlayer, Component title) {
    this(container, invPlayer, title, DM_FURNACE, 178, 165, 57);
  }

  public GUIDMFurnace(
      CONTAINER container,
      Inventory invPlayer,
      Component title,
      Identifier texture,
      int textureWidth,
      int textureHeight,
      int labelX) {
    super(container, invPlayer, title, textureWidth, textureHeight);
    this.texture = texture;
    this.furnace = container.furnace;
    this.titleLabelX = labelX;
    this.inventoryLabelX = labelX;
    this.inventoryLabelY = this.imageHeight - 94;
  }

  protected int getLitX() {
    return 49;
  }

  public void extractContents(
      @NotNull GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
    this.blit(
        graphics, this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    if (this.furnace.isLit()) {
      int litProgress = Mth.ceil(this.furnace.getLitProgress() * 11.0F) + 1;
      int litPortion = 14 - litProgress;
      graphics.blitSprite(
          RenderPipelines.GUI_TEXTURED,
          LIT_PROGRESS_SPRITE,
          14,
          14,
          0,
          litPortion,
          this.leftPos + this.getLitX(),
          this.topPos + 36 + litPortion,
          14,
          litProgress);
    }

    int burnProgress = Mth.ceil(this.furnace.getBurnProgress() * 24.0F);
    this.renderBurnProgress(graphics, burnProgress);
    super.extractContents(graphics, x, y, partialTicks);
  }

  protected void renderBurnProgress(@NotNull GuiGraphicsExtractor graphics, int burnProgress) {
    this.blit(
        graphics, this.texture, this.leftPos + 73, this.topPos + 34, 179, 14, burnProgress, 16);
  }
}
