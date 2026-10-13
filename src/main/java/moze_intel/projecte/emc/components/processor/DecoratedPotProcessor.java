package moze_intel.projecte.emc.components.processor;

import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class DecoratedPotProcessor extends PersistentComponentProcessor<PotDecorations> {
  @DataComponentProcessor.Instance
  public static final DecoratedPotProcessor INSTANCE = new DecoratedPotProcessor();

  private static final ResourceKey<Item> DECORATED_POT =
      (ResourceKey<Item>) BuiltInRegistries.ITEM.getResourceKey(Items.DECORATED_POT).orElseThrow();
  private long undecoratedEmc;

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_DECORATED_POT.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_DECORATED_POT.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_DECORATED_POT.tooltip();
  }

  protected @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info,
      @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC,
      @NotNull PotDecorations decorations)
      throws ArithmeticException {
    long totalDecorationEmc = 0L;

    for (Item decoration : decorations.ordered()) {
      long decorationEmc = IEMCProxy.INSTANCE.getValue(decoration);
      if (decorationEmc == 0L) {
        return 0L;
      }

      totalDecorationEmc = Math.addExact(totalDecorationEmc, decorationEmc);
    }

    return Math.addExact(currentEMC - this.undecoratedEmc, totalDecorationEmc);
  }

  @Override
  public void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    if (emcLookup == null) {
      this.undecoratedEmc = 0L;
    } else {
      this.undecoratedEmc = emcLookup.applyAsLong(ItemInfo.fromItem(Items.DECORATED_POT));
    }
  }

  @Override
  protected boolean validItem(@NotNull ItemInfo info) {
    return info.getItem().is(DECORATED_POT);
  }

  @Override
  protected DataComponentType<PotDecorations> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.POT_DECORATIONS;
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull PotDecorations component) {
    return !component.equals(PotDecorations.EMPTY);
  }
}
