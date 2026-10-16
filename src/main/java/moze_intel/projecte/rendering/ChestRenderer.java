package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import moze_intel.projecte.gameObjs.block_entities.EmcChestBlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockRegistryObject;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ChestRenderer<BE extends EmcChestBlockEntity>
    implements BlockEntityRenderer<BE, ChestRenderer.State> {
  private final ModelPart lid;
  private final ModelPart bottom;
  private final ModelPart lock;
  private final BlockRegistryObject<?, ?> type;
  private final RenderType renderType;

  public ChestRenderer(Context context, Identifier texture, BlockRegistryObject<?, ?> type) {
    this.type = type;
    this.renderType = RenderTypes.entityCutout(texture);
    ModelPart modelpart = context.bakeLayer(ModelLayers.CHEST);
    this.bottom = modelpart.getChild("bottom");
    this.lid = modelpart.getChild("lid");
    this.lock = modelpart.getChild("lock");
  }

  public ChestRenderer.State createRenderState() {
    return new ChestRenderer.State();
  }

  public void extractRenderState(
      BE chest,
      ChestRenderer.State state,
      float partialTick,
      Vec3 cameraPos,
      @Nullable CrumblingOverlay crumblingOverlay) {
    // The base renderer fills block position and light data used by submitModelPart.
    BlockEntityRenderer.super.extractRenderState(
        chest, state, partialTick, cameraPos, crumblingOverlay);
    BlockState blockState = chest.getBlockState();
    state.hasFacing =
        blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            && blockState.is(this.type.getBlock());
    state.facingDegrees =
        state.hasFacing
            ? -((Direction) blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)).toYRot()
            : 0.0F;
    float lidAngle = 1.0F - chest.getOpenNess(partialTick);
    state.lidRotation = -((1.0F - lidAngle * lidAngle * lidAngle) * (float) (Math.PI / 2));
  }

  public void submit(
      ChestRenderer.State state,
      PoseStack matrix,
      SubmitNodeCollector renderer,
      CameraRenderState cameraState) {
    matrix.pushPose();
    if (state.hasFacing) {
      matrix.translate(0.5, 0.5, 0.5);
      matrix.mulPose(Axis.YP.rotationDegrees(state.facingDegrees));
      matrix.translate(-0.5, -0.5, -0.5);
    }

    this.lid.xRot = state.lidRotation;
    this.lock.xRot = state.lidRotation;
    renderer.submitModelPart(
        this.lid, matrix, this.renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
    renderer.submitModelPart(
        this.lock, matrix, this.renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
    renderer.submitModelPart(
        this.bottom, matrix, this.renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
    matrix.popPose();
  }

  public static final class State extends BlockEntityRenderState {
    private boolean hasFacing;
    private float facingDegrees;
    private float lidRotation;
  }
}
