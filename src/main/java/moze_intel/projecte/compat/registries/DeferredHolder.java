package moze_intel.projecte.compat.registries;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeferredHolder<R, T extends R> {
  private final ResourceKey<R> key;
  @Nullable private DeferredRegister<R> owner;
  @Nullable private Supplier<? extends T> supplier;
  private boolean initialized;
  @Nullable private T value;
  @Nullable private Reference<T> holder;

  protected DeferredHolder(ResourceKey<R> key) {
    this.key = key;
  }

  protected DeferredHolder(ResourceKey<? extends Registry<R>> registryKey, Identifier id) {
    this(ResourceKey.create(registryKey, id));
  }

  public static <R, T extends R> DeferredHolder<R, T> create(
      ResourceKey<? extends Registry<R>> registryKey, Identifier id) {
    return new DeferredHolder<>(registryKey, id);
  }

  void bind(DeferredRegister<R> owner, Supplier<? extends T> supplier) {
    this.owner = owner;
    this.supplier = supplier;
  }

  @Nullable
  T createFromSupplier() {
    return (T) (this.supplier == null ? null : this.supplier.get());
  }

  void setValue(@NotNull T value, @Nullable Reference<T> holder) {
    this.value = value;
    this.holder = holder;
    this.initialized = true;
  }

  void markInitialized() {
    this.initialized = true;
  }

  @NotNull
  public ResourceKey<R> getKey() {
    return this.key;
  }

  @NotNull
  public Optional<Reference<T>> holder() {
    this.get();
    return Optional.ofNullable(this.holder);
  }

  public boolean isBound() {
    return this.get() != null;
  }

  @Nullable
  public T value() {
    return this.get();
  }

  @Nullable
  public T get() {
    if (!this.initialized) {
      if (this.owner == null) {
        this.resolveStandalone();
      } else {
        this.owner.acquire(this);
      }
    }

    return this.value;
  }

  private void resolveStandalone() {
    Registry<?> rawRegistry =
        (Registry<?>) BuiltInRegistries.REGISTRY.getValue(this.key.registryKey().identifier());
    if (rawRegistry != null) {
      Registry<R> registry = (Registry<R>) rawRegistry;
      Optional<R> registeredValue = registry.getOptional(this.key);
      if (!registeredValue.isEmpty()) {
        this.value = (T) registeredValue.get();
        Optional<Reference<R>> reference = registry.get(this.key);
        this.holder = (Reference<T>) (Reference<?>) reference.orElse(null);
        this.initialized = true;
      }
    }
  }

  @NotNull
  public T getOrThrow() {
    return this.get();
  }

  @NotNull
  public Identifier getId() {
    return this.key.identifier();
  }

  @NotNull
  public ResourceKey<? extends Registry<R>> getRegistryKey() {
    return this.key.registryKey();
  }

  @Override
  public String toString() {
    return "DeferredHolder[" + this.key + "]";
  }
}
