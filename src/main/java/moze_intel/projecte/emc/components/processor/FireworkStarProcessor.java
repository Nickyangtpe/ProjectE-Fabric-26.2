package moze_intel.projecte.emc.components.processor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMaps;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import java.util.Map.Entry;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IComponentProcessorHelper;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.emc.components.DataComponentManager;
import moze_intel.projecte.mixin.FireworkStarRecipeAccessor;
import moze_intel.projecte.utils.Constants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.FireworkExplosion.Shape;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class FireworkStarProcessor extends PersistentComponentProcessor<FireworkExplosion> {
  @DataComponentProcessor.Instance
  public static final FireworkStarProcessor INSTANCE = new FireworkStarProcessor();

  private static final ResourceKey<Item> FIREWORK_STAR =
      (ResourceKey<Item>) BuiltInRegistries.ITEM.getResourceKey(Items.FIREWORK_STAR).orElseThrow();
  private static final Int2ObjectMap<DyeColor> REVERSE_COLOR_LOOKUP =
      (Int2ObjectMap<DyeColor>)
          Util.make(
              new Int2ObjectOpenHashMap(),
              reverseLookup -> {
                for (DyeColor color : Constants.COLORS) {
                  reverseLookup.putIfAbsent(color.getFireworkColor(), color);
                }
              });
  @NotNull private Reference2LongMap<Shape> shapeEmcLookup = Reference2LongMaps.emptyMap();
  private long trailEmc;
  private long twinkleEmc;

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_FIREWORK_STAR.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_FIREWORK_STAR.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_FIREWORK_STAR.tooltip();
  }

  public @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info,
      @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC,
      @NotNull FireworkExplosion explosion)
      throws ArithmeticException {
    if ((!explosion.hasTrail() || this.trailEmc != 0L)
        && (!explosion.hasTwinkle() || this.twinkleEmc != 0L)) {
      Shape shape = explosion.shape();
      if (shape != Shape.SMALL_BALL) {
        long shapeEmc = this.shapeEmcLookup.getLong(shape);
        if (shapeEmc == 0L) {
          return 0L;
        }

        currentEMC = Math.addExact(currentEMC, shapeEmc);
      }

      if (explosion.hasTrail()) {
        currentEMC = Math.addExact(currentEMC, this.trailEmc);
      }

      if (explosion.hasTwinkle()) {
        currentEMC = Math.addExact(currentEMC, this.twinkleEmc);
      }

      IntList colors = explosion.colors();
      if (!colors.isEmpty()) {
        long colorsEmc = this.getUsedDyesEmc(colors);
        if (colorsEmc == 0L) {
          return 0L;
        }

        currentEMC = Math.addExact(currentEMC, colorsEmc);
      }

      IntList fadeColors = explosion.fadeColors();
      if (!fadeColors.isEmpty()) {
        long fadeEmc = this.getUsedDyesEmc(fadeColors);
        if (fadeEmc == 0L) {
          return 0L;
        }

        currentEMC = Math.addExact(currentEMC, fadeEmc);
      }

      return currentEMC;
    } else {
      return 0L;
    }
  }

  private long getUsedDyesEmc(IntList colors) {
    long totalEmc = 0L;
    IntListIterator iterator = colors.iterator();

    while (iterator.hasNext()) {
      int color = iterator.nextInt();
      DyeColor dyeColor = (DyeColor) REVERSE_COLOR_LOOKUP.get(color);
      if (dyeColor == null) {
        return 0L;
      }

      long dyeEmc = IComponentProcessorHelper.INSTANCE.getColorEmc(dyeColor);
      if (dyeEmc == 0L) {
        return 0L;
      }

      totalEmc = Math.addExact(totalEmc, dyeEmc);
    }

    return totalEmc;
  }

  @Override
  public void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    if (emcLookup == null) {
      this.shapeEmcLookup = Reference2LongMaps.emptyMap();
      this.trailEmc = this.twinkleEmc = 0L;
    } else {
      this.shapeEmcLookup = new Reference2LongOpenHashMap(Shape.values().length - 1);
      this.trailEmc = this.twinkleEmc = 0L;

      for (RecipeHolder<?> holder : DataComponentManager.getRecipeHolders()) {
        if (holder.value() instanceof FireworkStarRecipe recipe) {
          FireworkStarRecipeAccessor accessor = (FireworkStarRecipeAccessor) recipe;

          for (Entry<Shape, Ingredient> entry : accessor.projecte$getShapes().entrySet()) {
            Shape shape = entry.getKey();
            if (shape != Shape.SMALL_BALL) {
              long emc =
                  IComponentProcessorHelper.INSTANCE.getMinEmcFor(emcLookup, entry.getValue());
              if (emc > 0L) {
                this.shapeEmcLookup.mergeLong(shape, emc, Math::min);
              }
            }
          }

          long recipeTrailEmc =
              IComponentProcessorHelper.INSTANCE.getMinEmcFor(
                  emcLookup, accessor.projecte$getTrail());
          if (recipeTrailEmc > 0L && (this.trailEmc == 0L || recipeTrailEmc < this.trailEmc)) {
            this.trailEmc = recipeTrailEmc;
          }

          long recipeTwinkleEmc =
              IComponentProcessorHelper.INSTANCE.getMinEmcFor(
                  emcLookup, accessor.projecte$getTwinkle());
          if (recipeTwinkleEmc > 0L
              && (this.twinkleEmc == 0L || recipeTwinkleEmc < this.twinkleEmc)) {
            this.twinkleEmc = recipeTwinkleEmc;
          }
        }
      }
    }
  }

  @Override
  protected boolean validItem(@NotNull ItemInfo info) {
    return info.getItem().is(FIREWORK_STAR);
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull FireworkExplosion component) {
    return !component.equals(FireworkExplosion.DEFAULT);
  }

  @Override
  protected DataComponentType<FireworkExplosion> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.FIREWORK_EXPLOSION;
  }
}
