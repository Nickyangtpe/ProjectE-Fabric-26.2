package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class EntitySpriteRenderer<ENTITY extends Entity>
    extends EntityRenderer<ENTITY, EntityRenderState> {
  private final RenderType renderType;

  public EntitySpriteRenderer(Context context, Identifier texture) {
    super(context);
    this.renderType = PERenderType.SPRITE_RENDERER.apply(texture);
  }

  public EntityRenderState createRenderState() {
    return new EntityRenderState();
  }

  protected int getBlockLightLevel(@NotNull ENTITY entity, @NotNull BlockPos pos) {
    return 15;
  }

  public void submit(
      EntityRenderState state,
      PoseStack matrix,
      SubmitNodeCollector collector,
      CameraRenderState cameraState) {
    super.submit(state, matrix, collector, cameraState);
    matrix.pushPose();
    matrix.scale(0.5F, 0.5F, 0.5F);
    matrix.mulPose(cameraState.orientation);
    collector.submitCustomGeometry(
        matrix,
        this.renderType,
        (pose, builder) -> {
          vertex(builder, pose, 0.0F, 0.0F, 0.0F, 1.0F);
          vertex(builder, pose, 1.0F, 0.0F, 1.0F, 1.0F);
          vertex(builder, pose, 1.0F, 1.0F, 1.0F, 0.0F);
          vertex(builder, pose, 0.0F, 1.0F, 0.0F, 0.0F);
        });
    matrix.popPose();
  }

  private static void vertex(
      VertexConsumer consumer, Pose pose, float x, float y, float u, float v) {
    consumer
        .addVertex(pose, x - 0.5F, y, 0.0F)
        .setColor(255, 255, 255, 255)
        .setUv(u, v)
        .setUv1(0, 0)
        .setLight(15728880)
        .setNormal(pose, 0.0F, 0.0F, 1.0F)
        .setLineWidth(1.0F);
  }
}
