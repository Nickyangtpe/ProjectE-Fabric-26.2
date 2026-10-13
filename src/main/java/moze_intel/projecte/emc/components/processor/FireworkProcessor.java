package moze_intel.projecte.emc.components.processor;

import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IComponentProcessorHelper;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.emc.components.DataComponentManager;
import moze_intel.projecte.mixin.FireworkRocketRecipeAccessor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.FireworkRocketRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class FireworkProcessor extends PersistentComponentProcessor<Fireworks> {
  @DataComponentProcessor.Instance
  public static final FireworkProcessor INSTANCE = new FireworkProcessor();

  private static final ResourceKey<Item> FIREWORK_ROCKET =
      (ResourceKey<Item>)
          BuiltInRegistries.ITEM.getResourceKey(Items.FIREWORK_ROCKET).orElseThrow();
  private long powderEmc;

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_FIREWORK.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_FIREWORK.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_FIREWORK.tooltip();
  }

  protected @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info,
      @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC,
      @NotNull Fireworks fireworks)
      throws ArithmeticException {
    int flightDuration = fireworks.flightDuration();
    if (flightDuration > 1) {
      if (this.powderEmc == 0L) {
        return 0L;
      }

      currentEMC =
          Math.addExact(currentEMC, Math.multiplyExact(flightDuration - 1, this.powderEmc));
    }

    for (FireworkExplosion explosion : fireworks.explosions()) {
      long starEmc =
          IEMCProxy.INSTANCE.getValue(
              ItemInfo.fromItem(
                  Items.FIREWORK_STAR,
                  DataComponentPatch.builder()
                      .set(DataComponents.FIREWORK_EXPLOSION, explosion)
                      .build()));
      if (starEmc == 0L) {
        return 0L;
      }

      currentEMC = Math.addExact(currentEMC, starEmc);
    }

    return currentEMC;
  }

  @Override
  public void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    this.powderEmc = 0L;
    if (emcLookup != null) {
      for (RecipeHolder<?> holder : DataComponentManager.getRecipeHolders()) {
        if (holder.value() instanceof FireworkRocketRecipe recipe) {
          long value =
              IComponentProcessorHelper.INSTANCE.getMinEmcFor(
                  emcLookup, ((FireworkRocketRecipeAccessor) recipe).projecte$getFuel());
          if (value > 0L && (this.powderEmc == 0L || value < this.powderEmc)) {
            this.powderEmc = value;
          }
        }
      }
    }
  }

  @Override
  protected boolean validItem(@NotNull ItemInfo info) {
    return info.getItem().is(FIREWORK_ROCKET);
  }

  @Override
  protected DataComponentType<Fireworks> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.FIREWORKS;
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull Fireworks component) {
    return component.flightDuration() != 1 || !component.explosions().isEmpty();
  }
}
