package moze_intel.projecte.api.proxy;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.IComponentProcessorHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface IEMCProxy extends ToLongFunction<ItemInfo> {
  IEMCProxy INSTANCE =
      ServiceLoader.load(IEMCProxy.class)
          .findFirst()
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "No valid ServiceImpl for IEMCProxy found, ProjectE may be absent, damaged,"
                          + " or outdated"));

  default boolean hasValue(@NotNull ItemLike itemLike) {
    return this.getValue(itemLike) > 0L;
  }

  default boolean hasValue(@NotNull Holder<Item> holder) {
    return this.getValue(holder) > 0L;
  }

  default boolean hasValue(@NotNull ItemStack stack) {
    return this.getValue(stack) > 0L;
  }

  default boolean hasValue(@NotNull ItemInfo info) {
    return this.getValue(info) > 0L;
  }

  default @Range(from = 0L, to = Long.MAX_VALUE) long getValue(@NotNull ItemLike itemLike) {
    Item item = Objects.requireNonNull(itemLike).asItem();
    return item == Items.AIR ? 0L : this.getValue(ItemInfo.fromItem(item));
  }

  default @Range(from = 0L, to = Long.MAX_VALUE) long getValue(@NotNull Holder<Item> holder) {
    Objects.requireNonNull(holder);
    return holder.isBound() && holder.value() != Items.AIR
        ? this.getValue(ItemInfo.fromItem(holder))
        : 0L;
  }

  default @Range(from = 0L, to = Long.MAX_VALUE) long getValue(@NotNull ItemStack stack) {
    return Objects.requireNonNull(stack).isEmpty() ? 0L : this.getValue(ItemInfo.fromStack(stack));
  }

  @Range(from = 0L, to = Long.MAX_VALUE)
  long getValue(@NotNull ItemInfo var1);

  default @Range(from = 0L, to = Long.MAX_VALUE) long applyAsLong(@NotNull ItemInfo info) {
    return this.getValue(info);
  }

  default @Range(from = 0L, to = Long.MAX_VALUE) long getSellValue(@NotNull ItemStack stack) {
    return Objects.requireNonNull(stack).isEmpty()
        ? 0L
        : this.getSellValue(ItemInfo.fromStack(stack));
  }

  @Range(from = 0L, to = Long.MAX_VALUE)
  long getSellValue(@NotNull ItemInfo var1);

  @NotNull
  default ItemInfo getPersistentInfo(@NotNull ItemInfo info) {
    return IComponentProcessorHelper.INSTANCE.getPersistentInfo(info);
  }
}
