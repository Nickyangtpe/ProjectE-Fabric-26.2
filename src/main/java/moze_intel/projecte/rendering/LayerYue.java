package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class LayerYue extends RenderLayer<AvatarRenderState, PlayerModel> {
  private static final UUID SIN_UUID = UUID.fromString("5f86012c-ca4b-451a-989c-8fab167af647");
  private static final UUID CLAR_UUID = UUID.fromString("e5c59746-9cf7-4940-a849-d09e1f1efc13");
  private static final Identifier HEART_LOC = PECore.rl("textures/models/heartcircle.png");
  private static final Identifier YUE_LOC = PECore.rl("textures/models/yuecircle.png");
  private static final Map<Integer, UUID> PLAYER_UUIDS = new ConcurrentHashMap<>();

  public LayerYue(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
    super(renderer);
  }

  public static void register() {
    LivingEntityRenderLayerRegistrationCallback.EVENT.register(
        (LivingEntityRenderLayerRegistrationCallback)
            (entityType, renderer, helper, context) -> {
              if (renderer instanceof AvatarRenderer) {
                RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                    (RenderLayerParent<AvatarRenderState, PlayerModel>) renderer;
                helper.register(new LayerYue(parent));
              }
            });
  }

  public static void track(Entity entity) {
    if (entity instanceof Player player) {
      PLAYER_UUIDS.put(entity.getId(), player.getUUID());
    }
  }

  public static void untrack(Entity entity) {
    PLAYER_UUIDS.remove(entity.getId());
  }

  public void submit(
      PoseStack matrix,
      SubmitNodeCollector renderer,
      int light,
      AvatarRenderState state,
      float limbSwing,
      float limbSwingAmount) {
    if (!state.isInvisible) {
      UUID uuid = PLAYER_UUIDS.get(state.id);
      if (!FMLEnvironment.production || SIN_UUID.equals(uuid) || CLAR_UUID.equals(uuid)) {
        matrix.pushPose();
        ((PlayerModel) this.getParentModel()).body.translateAndRotate(matrix);
        double yShift = -0.498;
        if (state.isCrouching) {
          matrix.mulPose(Axis.XN.rotation(0.5F));
          yShift = -0.44;
        }

        matrix.mulPose(Axis.ZP.rotation((float) Math.PI));
        matrix.scale(3.0F, 3.0F, 3.0F);
        matrix.translate(-0.5, yShift, -0.5);
        Identifier texture = CLAR_UUID.equals(uuid) ? HEART_LOC : YUE_LOC;
        renderer.submitCustomGeometry(
            matrix,
            PERenderType.YUE_RENDERER.apply(texture),
            (pose, builder) -> renderQuad(builder, pose));
        matrix.popPose();
      }
    }
  }

  private static void renderQuad(VertexConsumer builder, Pose pose) {
    builder.addVertex(pose, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0, 1, 0, 1);
    builder.addVertex(pose, 0.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F).setColor(0, 1, 0, 1);
    builder.addVertex(pose, 1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F).setColor(0, 1, 0, 1);
    builder.addVertex(pose, 1.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(0, 1, 0, 1);
  }
}
