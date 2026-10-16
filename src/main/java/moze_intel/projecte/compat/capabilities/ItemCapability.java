package moze_intel.projecte.compat.capabilities;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemCapability<T, C> {
  private final ItemApiLookup<T, C> lookup;

  private ItemCapability(ItemApiLookup<T, C> lookup) {
    this.lookup = lookup;
  }

  public static <T, C> ItemCapability<T, C> createVoid(Identifier id, Class<T> apiClass) {
    Class<C> contextClass = (Class<C>) Void.class;
    return new ItemCapability<>(ItemApiLookup.get(id, apiClass, contextClass));
  }

  public static <T, C> ItemCapability<T, C> create(
      Identifier id, Class<T> apiClass, Class<C> contextClass) {
    return new ItemCapability<>(ItemApiLookup.get(id, apiClass, contextClass));
  }

  @NotNull
  public ItemApiLookup<T, C> lookup() {
    return this.lookup;
  }

  @Nullable
  public T find(@NotNull ItemStack stack, @Nullable C context) {
    return (T) this.lookup.find(stack, context);
  }

  public void registerForItems(ICapabilityProvider<ItemStack, C, T> provider, Item... items) {
    this.lookup.registerForItems(
        (stack, context) -> provider.getCapability(stack, (C) context), items);
  }
}
