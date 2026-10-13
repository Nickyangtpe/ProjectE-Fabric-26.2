package moze_intel.projecte.compat.registries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.compat.eventbus.IEventBus;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeferredRegister<T> {
  private static final List<DeferredRegister<?>> ALL = new ArrayList<>();
  private static final Map<ResourceKey<? extends Registry<?>>, Integer> REGISTRY_ORDER =
      new HashMap<>();
  private static final Map<ResourceKey<?>, Registry<?>> EXTERNAL_REGISTRIES = new HashMap<>();
  private final ResourceKey<? extends Registry<T>> registryKey;
  private final String namespace;
  private final List<DeferredHolder<T, ? extends T>> entries = new ArrayList<>();
  @Nullable private Registry<T> registry;

  protected DeferredRegister(
      ResourceKey<? extends Registry<T>> registryKey, @NotNull String namespace) {
    this.registryKey = registryKey;
    this.namespace = namespace;
  }

  public static <B> DeferredRegister<B> create(
      ResourceKey<? extends Registry<B>> registryKey, String namespace) {
    return new DeferredRegister<B>((ResourceKey<? extends Registry<B>>) registryKey, namespace);
  }

  public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
    return this.register(name, id -> sup.get());
  }

  public <I extends T> DeferredHolder<T, I> register(
      String name, Function<Identifier, ? extends I> func) {
    Identifier id = Identifier.fromNamespaceAndPath(this.namespace, name);
    DeferredHolder<T, I> holder = this.createHolder(this.registryKey, id);
    holder.bind(this, () -> func.apply(id));
    this.entries.add(holder);
    return holder;
  }

  protected <I extends T> DeferredHolder<T, I> createHolder(
      ResourceKey<? extends Registry<T>> registryKey, Identifier key) {
    return new DeferredHolder<>(ResourceKey.create(registryKey, key));
  }

  public void register(IEventBus bus) {
    synchronized (ALL) {
      if (!ALL.contains(this)) {
        ALL.add(this);
      }
    }
  }

  @NotNull
  public Collection<DeferredHolder<T, ? extends T>> getEntries() {
    return this.entries;
  }

  public static void registerAll() {
    List<DeferredRegister<?>> ordered;
    synchronized (ALL) {
      ordered = new ArrayList<>(ALL);
    }

    ordered.sort(Comparator.comparingInt(DeferredRegister::order));

    for (DeferredRegister<?> register : ordered) {
      for (DeferredHolder<?, ?> holder : register.entries) {
        holder.get();
      }
    }
  }

  public static <T> void registerExternal(
      ResourceKey<? extends Registry<T>> key, Registry<T> registry) {
    synchronized (EXTERNAL_REGISTRIES) {
      EXTERNAL_REGISTRIES.put(key, registry);
    }
  }

  private static int order(DeferredRegister<?> register) {
    return REGISTRY_ORDER.getOrDefault(register.registryKey, 1000);
  }

  void acquire(DeferredHolder<T, ?> holder) {
    T value = holder.createFromSupplier();
    if (value == null) {
      holder.markInitialized();
    } else {
      Reference<T> ref = Registry.registerForHolder(this.getRegistry(), holder.getKey(), value);
      ((DeferredHolder<T, T>) holder).setValue(value, ref);
    }
  }

  @NotNull
  private Registry<T> getRegistry() {
    synchronized (EXTERNAL_REGISTRIES) {
      Registry<?> external = EXTERNAL_REGISTRIES.get(this.registryKey);
      if (external != null) {
        this.registry = (Registry<T>) external;
        return this.registry;
      }
    }

    if (this.registry == null) {
      Registry<?> found =
          (Registry<?>) BuiltInRegistries.REGISTRY.getValue(this.registryKey.identifier());
      if (found == null) {
        throw new IllegalStateException("Unknown registry " + this.registryKey);
      }

      this.registry = (Registry<T>) found;
    }

    return this.registry;
  }

  static {
    int order = 0;

    for (ResourceKey<? extends Registry<?>> key :
        List.of(
            Registries.BLOCK,
            Registries.ITEM,
            Registries.BLOCK_ENTITY_TYPE,
            Registries.ENTITY_TYPE,
            Registries.MENU,
            Registries.SOUND_EVENT,
            Registries.DATA_COMPONENT_TYPE,
            Registries.RECIPE_SERIALIZER,
            Registries.CREATIVE_MODE_TAB,
            Registries.COMMAND_ARGUMENT_TYPE)) {
      REGISTRY_ORDER.put(key, order++);
    }
  }
}
