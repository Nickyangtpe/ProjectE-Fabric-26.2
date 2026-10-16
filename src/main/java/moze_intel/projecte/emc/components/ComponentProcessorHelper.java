package moze_intel.projecte.emc.components;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.IComponentProcessorHelper;
import moze_intel.projecte.utils.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class ComponentProcessorHelper implements IComponentProcessorHelper {
  private static final Function<Holder<Item>, ItemInfo> HOLDER_TO_INFO = ItemInfo::fromItem;
  private static final Function<ItemStack, @Nullable ItemInfo> STACK_TO_INFO =
      stack -> stack.isEmpty() ? null : ItemInfo.fromStack(stack);
  @Nullable private Object2LongMap<DyeColor> colorEmc;

  public static ComponentProcessorHelper instance() {
    return (ComponentProcessorHelper) IComponentProcessorHelper.INSTANCE;
  }

  void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    if (emcLookup == null) {
      this.colorEmc = null;
    } else {
      this.colorEmc = new Object2LongOpenHashMap();

      for (DyeColor color : Constants.COLORS) {
        TagKey<Item> dyeTag =
            TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("c", "dyes/" + color.getSerializedName()));
        long minColorEmc =
            this.getMinValue(
                emcLookup, BuiltInRegistries.ITEM.getTagOrEmpty(dyeTag), HOLDER_TO_INFO);
        if (minColorEmc > 0L) {
          this.colorEmc.put(color, minColorEmc);
        }
      }
    }
  }

  @NotNull
  @Override
  public ItemInfo getPersistentInfo(@NotNull ItemInfo info) {
    return DataComponentManager.getPersistentInfo(info);
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long getColorEmc(@NotNull DyeColor color) {
    return this.colorEmc == null ? 0L : this.colorEmc.getLong(color);
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long getMinEmcFor(
      @NotNull ToLongFunction<ItemInfo> emcLookup, @NotNull Ingredient ingredient) {
    return this.getMinValue(
        emcLookup, Arrays.asList(this.getMatchingStacks(ingredient)), STACK_TO_INFO);
  }

  private ItemStack[] getMatchingStacks(Ingredient ingredient) {
    return ingredient
        .items()
        .map(holder -> ((Item) holder.value()).getDefaultInstance())
        .toArray(ItemStack[]::new);
  }

  private <TYPE> @Range(from = 0L, to = Long.MAX_VALUE) long getMinValue(
      @NotNull ToLongFunction<ItemInfo> emcLookup,
      @NotNull Iterable<TYPE> iterable,
      @NotNull Function<TYPE, @Nullable ItemInfo> infoCreator) {
    long minEmc = 0L;

    for (TYPE type : iterable) {
      ItemInfo info = infoCreator.apply(type);
      if (info != null) {
        long enc = emcLookup.applyAsLong(info);
        if (enc != 0L && (minEmc == 0L || enc < minEmc)) {
          minEmc = enc;
        }
      }
    }

    return minEmc;
  }
}
