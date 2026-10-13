package moze_intel.projecte.compat.capabilities;

import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EntityCapability<T, C> {
  private final EntityApiLookup<T, C> lookup;

  private EntityCapability(EntityApiLookup<T, C> lookup) {
    this.lookup = lookup;
  }

  public static <T, C> EntityCapability<T, C> createVoid(Identifier id, Class<T> apiClass) {
    Class<C> contextClass = (Class<C>) Void.class;
    return new EntityCapability<>(EntityApiLookup.get(id, apiClass, contextClass));
  }

  public static <T, C> EntityCapability<T, C> create(
      Identifier id, Class<T> apiClass, Class<C> contextClass) {
    return new EntityCapability<>(EntityApiLookup.get(id, apiClass, contextClass));
  }

  @NotNull
  public EntityApiLookup<T, C> lookup() {
    return this.lookup;
  }

  @Nullable
  public T find(@NotNull Entity entity, @Nullable C context) {
    return (T) this.lookup.find(entity, context);
  }

  public <E extends Entity> void registerForType(
      ICapabilityProvider<E, C, T> provider, EntityType<E> type) {
    this.lookup.registerForType(
        (entity, context) -> provider.getCapability((E) entity, (C) context), type);
  }
}
