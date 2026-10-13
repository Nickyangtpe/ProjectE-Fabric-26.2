package moze_intel.projecte.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.function.Function;
import moze_intel.projecte.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class PERenderType {
  public static final RenderPipeline SPRITE_PIPELINE = RenderPipelines.ENTITY_CUTOUT;
  public static final RenderPipeline YUE_PIPELINE = RenderPipelines.ENTITY_TRANSLUCENT;
  public static final RenderPipeline TRANSMUTATION_PIPELINE = RenderPipelines.DEBUG_QUADS;
  public static final Function<Identifier, RenderType> SPRITE_RENDERER =
      Util.memoize(texture -> RenderTypes.entityCutout(texture));
  public static final Function<Identifier, RenderType> YUE_RENDERER =
      Util.memoize(texture -> RenderTypes.entityTranslucent(texture));
  public static final RenderType TRANSMUTATION_OVERLAY =
      RenderTypeAccessor.projecte$create(
          "projecte_transmutation_overlay",
          RenderSetup.builder(TRANSMUTATION_PIPELINE)
              .sortOnUpload()
              .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
              .createRenderSetup());

  private PERenderType() {}
}
