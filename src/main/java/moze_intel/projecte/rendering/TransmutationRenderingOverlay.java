package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.Constants;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TransmutationRenderingOverlay {
  private static final Identifier HUD_ID = PECore.rl("transmutation_preview");
  private volatile TransmutationRenderingOverlay.OverlayFrame frame =
      TransmutationRenderingOverlay.OverlayFrame.EMPTY;

  private TransmutationRenderingOverlay() {}

  public static void register() {
    TransmutationRenderingOverlay overlay = new TransmutationRenderingOverlay();
    LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register(overlay::extractPreview);
    LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(overlay::submitPreview);
    HudElementRegistry.attachElementAfter(
        VanillaHudElements.CROSSHAIR, HUD_ID, overlay::extractHud);
  }

  private void extractPreview(LevelExtractionContext context, HitResult outlineHit) {
    Camera camera = context.camera();
    if (camera.entity() instanceof Player player) {
      ClientLevel level = context.level();
      long gameTime = level.getGameTime();
      ItemStack stack = player.getMainHandItem();
      if (stack.isEmpty()) {
        stack = player.getOffhandItem();
      }

      if (!stack.isEmpty() && stack.is(PEItems.PHILOSOPHERS_STONE.get())) {
        boolean isSneaking = player.isSecondaryUseActive();
        PhilosophersStone stone = (PhilosophersStone) stack.getItem();
        BlockHitResult hit = stone.getHitBlock(level, player, isSneaking);
        if (hit.getType() != Type.BLOCK) {
          this.frame =
              new TransmutationRenderingOverlay.OverlayFrame(null, null, List.of(), gameTime, 0.0F);
        } else {
          int charge = stone.getCharge(stack);
          PhilosophersStone.PhilosophersStoneMode mode = stone.getMode(stack);
          Object2ReferenceMap<BlockPos, BlockState> changes =
              PhilosophersStone.getChanges(
                  level,
                  hit.getBlockPos(),
                  hit.getDirection(),
                  player.getDirection(),
                  isSneaking,
                  mode,
                  charge);
          if (changes.isEmpty()) {
            this.frame =
                new TransmutationRenderingOverlay.OverlayFrame(
                    null, null, List.of(), gameTime, 0.0F);
          } else {
            Block result = ((BlockState) changes.values().iterator().next()).getBlock();
            BlockPos resultPos = ((BlockPos) changes.keySet().iterator().next()).immutable();
            float alpha =
                ProjectEConfig.client.pulsatingOverlay.get() ? getPulseProportion() * 0.6F : 0.35F;
            CollisionContext selectionContext = CollisionContext.of(player);
            List<TransmutationRenderingOverlay.WorldShape> shapes = new ArrayList<>(changes.size());
            ObjectIterator var20 = changes.keySet().iterator();

            while (var20.hasNext()) {
              BlockPos pos = (BlockPos) var20.next();
              BlockState state = level.getBlockState(pos);
              if (!state.isAir()) {
                VoxelShape shape = state.getShape(level, pos, selectionContext);
                if (!shape.isEmpty()) {
                  shapes.add(new TransmutationRenderingOverlay.WorldShape(pos.immutable(), shape));
                }
              }
            }

            this.frame =
                new TransmutationRenderingOverlay.OverlayFrame(
                    result, resultPos, List.copyOf(shapes), gameTime, alpha);
          }
        }
      } else {
        this.frame =
            new TransmutationRenderingOverlay.OverlayFrame(null, null, List.of(), gameTime, 0.0F);
      }
    } else {
      this.frame = TransmutationRenderingOverlay.OverlayFrame.EMPTY;
    }
  }

  private boolean submitPreview(LevelRenderContext context, BlockOutlineRenderState outlineState) {
    TransmutationRenderingOverlay.OverlayFrame current = this.frame;
    if (!current.shapes().isEmpty()) {
      Vec3 camera = context.levelState().cameraRenderState.pos;
      PoseStack matrices = context.poseStack();

      for (TransmutationRenderingOverlay.WorldShape worldShape : current.shapes()) {
        BlockPos pos = worldShape.pos();
        matrices.pushPose();
        matrices.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        context
            .submitNodeCollector()
            .submitCustomGeometry(
                matrices,
                PERenderType.TRANSMUTATION_OVERLAY,
                (pose, consumer) ->
                    renderShape(worldShape.shape(), pose, consumer, current.alpha()));
        matrices.popPose();
      }
    }

    return true;
  }

  private void extractHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
    TransmutationRenderingOverlay.OverlayFrame current = this.frame;
    Minecraft minecraft = Minecraft.getInstance();
    if (!minecraft.gui.hud.isHidden() && current.result() != null) {
      if (current.result() instanceof LiquidBlock liquidBlock && minecraft.level != null) {
        BlockState fluidBlockState = liquidBlock.defaultBlockState();
        FluidModel model =
            minecraft
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluidBlockState.getFluidState());
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int color =
            model.tintSource() == null
                ? -1
                : model
                    .tintSource()
                    .colorInWorld(fluidBlockState, minecraft.level, current.resultPos());
        if (color >>> 24 == 0) {
          color |= -16777216;
        }

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 1, 1, 16, 16, color);
      } else {
        graphics.item(new ItemStack(current.result()), 1, 1);
      }

      long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
      if (current.gameTime() != gameTime) {
        this.frame = TransmutationRenderingOverlay.OverlayFrame.EMPTY;
      }
    }
  }

  private static void renderShape(
      VoxelShape shape, Pose pose, VertexConsumer consumer, float alpha) {
    shape.forAllBoxes(
        (minX, minY, minZ, maxX, maxY, maxZ) -> {
          for (Direction direction : Constants.DIRECTIONS) {
            switch (direction) {
              case DOWN:
                quad(
                    consumer, pose, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX,
                    minY, minZ, alpha);
                break;
              case UP:
                quad(
                    consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX,
                    maxY, maxZ, alpha);
                break;
              case NORTH:
                quad(
                    consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX,
                    maxY, minZ, alpha);
                break;
              case SOUTH:
                quad(
                    consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX,
                    minY, maxZ, alpha);
                break;
              case WEST:
                quad(
                    consumer, pose, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX,
                    minY, maxZ, alpha);
                break;
              case EAST:
                quad(
                    consumer, pose, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX,
                    maxY, minZ, alpha);
            }
          }
        });
  }

  private static void quad(
      VertexConsumer consumer,
      Pose pose,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      double x3,
      double y3,
      double z3,
      double x4,
      double y4,
      double z4,
      float alpha) {
    vertex(consumer, pose, x1, y1, z1, alpha);
    vertex(consumer, pose, x2, y2, z2, alpha);
    vertex(consumer, pose, x3, y3, z3, alpha);
    vertex(consumer, pose, x4, y4, z4, alpha);
  }

  private static void vertex(
      VertexConsumer consumer, Pose pose, double x, double y, double z, float alpha) {
    consumer.addVertex(pose, (float) x, (float) y, (float) z).setColor(1.0F, 1.0F, 1.0F, alpha);
  }

  private static float getPulseProportion() {
    return (float) (0.5 * Math.sin(System.currentTimeMillis() / 350.0) + 0.5);
  }

  private record OverlayFrame(
      @Nullable Block result,
      @Nullable BlockPos resultPos,
      List<TransmutationRenderingOverlay.WorldShape> shapes,
      long gameTime,
      float alpha) {
    private static final TransmutationRenderingOverlay.OverlayFrame EMPTY =
        new TransmutationRenderingOverlay.OverlayFrame(null, null, List.of(), 0L, 0.0F);
  }

  private record WorldShape(BlockPos pos, VoxelShape shape) {}
}
