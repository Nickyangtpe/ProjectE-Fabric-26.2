package moze_intel.projecte.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Kind;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemInfo {
  public static final MapCodec<ItemInfo> MAP_CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      BuiltInRegistries.ITEM
                          .holderByNameCodec()
                          .fieldOf("item")
                          .forGetter(ItemInfo::getItem),
                      DataComponentPatch.CODEC
                          .optionalFieldOf("data", DataComponentPatch.EMPTY)
                          .forGetter(ItemInfo::getComponentsPatch))
                  .apply(instance, ItemInfo::new));
  public static final Codec<ItemInfo> CODEC = MAP_CODEC.codec();
  public static final StreamCodec<RegistryFriendlyByteBuf, ItemInfo> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.holderRegistry(Registries.ITEM),
          ItemInfo::getItem,
          DataComponentPatch.STREAM_CODEC,
          ItemInfo::getComponentsPatch,
          ItemInfo::new);
  @NotNull private final Holder<Item> item;
  @NotNull private final DataComponentPatch componentsPatch;
  private boolean hasCachedHash;
  private int cachedHashCode;

  private ItemInfo(@NotNull Holder<Item> item, @NotNull DataComponentPatch componentsPatch) {
    this.item = item;
    this.componentsPatch = componentsPatch;
  }

  public static ItemInfo fromItem(
      @NotNull ItemLike itemLike, @NotNull DataComponentPatch componentsPatch) {
    return new ItemInfo(itemLike.asItem().builtInRegistryHolder(), componentsPatch);
  }

  public static ItemInfo fromItem(
      @NotNull Holder<Item> holder, @NotNull DataComponentPatch componentsPatch) {
    if (holder.kind() == Kind.DIRECT) {
      if (!holder.isBound()) {
        throw new IllegalArgumentException("ItemInfo does not support unbound direct holders.");
      }

      holder = BuiltInRegistries.ITEM.wrapAsHolder((Item) holder.value());
      if (holder.kind() == Kind.DIRECT) {
        throw new IllegalArgumentException(
            "ItemInfo does not support direct holders for unregistered items.");
      }
    }

    return new ItemInfo(holder, componentsPatch);
  }

  public static ItemInfo fromItem(@NotNull ItemLike itemLike) {
    return fromItem(itemLike.asItem().builtInRegistryHolder(), DataComponentPatch.EMPTY);
  }

  public static ItemInfo fromItem(@NotNull Holder<Item> holder) {
    return fromItem(holder, DataComponentPatch.EMPTY);
  }

  public static ItemInfo fromStack(@NotNull ItemStack stack) {
    return new ItemInfo(stack.typeHolder(), stack.getComponentsPatch());
  }

  @Nullable
  public static ItemInfo fromNSS(@NotNull NSSItem stack) {
    if (stack.representsTag()) {
      return null;
    }

    Optional<Reference<Item>> holder = BuiltInRegistries.ITEM.get(stack.getResourceLocation());
    return holder.isEmpty()
        ? null
        : fromItem((Holder<Item>) holder.get(), stack.getComponentsPatch());
  }

  public NSSItem toNSS() {
    return NSSItem.createItem(this.item, this.componentsPatch);
  }

  @NotNull
  public Holder<Item> getItem() {
    return this.item;
  }

  @NotNull
  public DataComponentPatch getComponentsPatch() {
    return this.componentsPatch;
  }

  public boolean hasModifiedComponents() {
    return !this.componentsPatch.isEmpty();
  }

  @Nullable
  public <T> T getOrNull(DataComponentType<? extends T> type) {
    return (T) this.componentsPatch.get(DataComponentMap.EMPTY, type);
  }

  public ItemInfo itemOnly() {
    return this.componentsPatch.isEmpty() ? this : fromItem(this.item);
  }

  public ItemStack createStack() {
    return new ItemStack(this.getItem(), 1, this.getComponentsPatch());
  }

  @Override
  public int hashCode() {
    if (!this.hasCachedHash) {
      this.hasCachedHash = true;
      ResourceKey<Item> resourceKey = (ResourceKey<Item>) this.item.unwrap().left().orElse(null);
      int code = resourceKey == null ? 0 : resourceKey.hashCode();
      this.cachedHashCode = 31 * code + this.componentsPatch.hashCode();
    }

    return this.cachedHashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else {
      return !(o instanceof ItemInfo other)
          ? false
          : this.item.is(other.item) && this.componentsPatch.equals(other.componentsPatch);
    }
  }

  @Override
  public String toString() {
    return this.componentsPatch.isEmpty()
        ? this.item.getRegisteredName()
        : this.item.getRegisteredName() + " " + this.componentsPatch;
  }
}
