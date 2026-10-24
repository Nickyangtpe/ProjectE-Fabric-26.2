package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import moze_intel.projecte.gameObjs.block_entities.DMPedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class PedestalRenderer
    implements BlockEntityRenderer<DMPedestalBlockEntity, PedestalRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  private final Context context;

  public PedestalRenderer(Context context) {
    this.context = context;
    this.itemModelResolver = context.itemModelResolver();
  }

  public PedestalRenderer.State createRenderState() {
    return new PedestalRenderer.State();
  }

  public void extractRenderState(
      DMPedestalBlockEntity pedestal,
      PedestalRenderer.State state,
      float partialTick,
      Vec3 cameraPos,
      @Nullable CrumblingOverlay crumblingOverlay) {
    // Populate block position and light coordinates before submitting the item state.
    // Without the base extraction, the item is resolved but submitted with an empty
    // render state and is invisible in-world.
    BlockEntityRenderer.super.extractRenderState(
        pedestal, state, partialTick, cameraPos, crumblingOverlay);
    Minecraft minecraft = Minecraft.getInstance();
    state.debugBounds = minecraft.options.keyDebugShowHitboxes.isDown();
    AABB itemEffectBounds = pedestal.getItemEffectBounds();
    state.rangePreview =
        itemEffectBounds != null
            && minecraft.hitResult instanceof BlockHitResult hit
            && hit.getType() == Type.BLOCK
            && hit.getBlockPos().equals(pedestal.getBlockPos());
    state.effectBounds = itemEffectBounds == null ? pedestal.getEffectBounds() : itemEffectBounds;
    state.active = pedestal.getActive();
    state.hasItem = false;
    state.itemRenderState.clear();
    if (!pedestal.isRemoved() && pedestal.getLevel() != null) {
      ItemStack stack = pedestal.getInventory().getStackInSlot(0);
      if (!stack.isEmpty()) {
        int seed = (int) pedestal.getBlockPos().asLong();
        this.itemModelResolver.updateForTopItem(
            state.itemRenderState,
            stack,
            ItemDisplayContext.GROUND,
            pedestal.getLevel(),
            null,
            seed);
        state.hasItem = !state.itemRenderState.isEmpty();
      }

      long gameTime = pedestal.getLevel().getGameTime();
      state.bobOffset = Mth.sin(((float) gameTime + partialTick) / 10.0F) * 0.1F + 0.1F;
      state.rotation = ((float) gameTime + partialTick) / 20.0F;
      state.tilt = Mth.sin(((float) gameTime + partialTick) / 16.0F) * 0.035F;
      state.rangePulse = Mth.sin(((float) gameTime + partialTick) / 6.0F) * 0.25F + 1.75F;
    }
  }

  public void submit(
      PedestalRenderer.State state,
      PoseStack matrix,
      SubmitNodeCollector renderer,
      CameraRenderState cameraState) {
    if (state.debugBounds || state.rangePreview) {
      BlockPos pos = state.blockPos;
      AABB localBounds = state.effectBounds.move(-pos.getX(), -pos.getY(), -pos.getZ());
      int color =
          state.rangePreview ? (state.active ? 0xE62FE6C4 : 0xE6FFB347) : 0xFFFF00FF;
      renderer.submitShapeOutline(
          matrix,
          Shapes.create(localBounds),
          state.rangePreview ? RenderTypes.linesTranslucent() : RenderTypes.lines(),
          color,
          state.rangePreview ? state.rangePulse : 1.0F,
          true);
      if (state.rangePreview) {
        AABB innerBounds = localBounds.deflate(0.06);
        AABB footprint =
            new AABB(
                localBounds.minX + 0.03,
                0.025,
                localBounds.minZ + 0.03,
                localBounds.maxX - 0.03,
                0.035,
                localBounds.maxZ - 0.03);
        int accent = state.active ? 0x8039FFE0 : 0x80FFD080;
        renderer.submitShapeOutline(
            matrix,
            Shapes.or(Shapes.create(innerBounds), Shapes.create(footprint)),
            RenderTypes.linesTranslucent(),
            accent,
            1.0F,
            true);
      }
    }

    if (state.hasItem) {
      matrix.pushPose();
      matrix.translate(0.5, 0.7 + state.bobOffset, 0.5);
      matrix.scale(0.8F, 0.8F, 0.8F);
      matrix.mulPose(Axis.YP.rotation(state.rotation));
      matrix.mulPose(Axis.XP.rotation(state.tilt));
      state.itemRenderState.submit(
          matrix,
          renderer,
          state.lightCoords,
          OverlayTexture.NO_OVERLAY,
          (int) state.blockPos.asLong());
      matrix.popPose();
    }
  }

  public static final class State extends BlockEntityRenderState {
    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    private AABB effectBounds = AABB.ofSize(Vec3.ZERO, 1.0, 1.0, 1.0);
    private boolean debugBounds;
    private boolean rangePreview;
    private boolean active;
    private boolean hasItem;
    private float bobOffset;
    private float rotation;
    private float tilt;
    private float rangePulse = 1.75F;
  }
}
