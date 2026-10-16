package moze_intel.projecte.gameObjs.registration;

import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.compat.registries.DeferredRegister;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

public class PEDeferredRegister<T> extends DeferredRegister<T> {
  @NotNull private final Function<ResourceKey<T>, ? extends PEDeferredHolder<T, ?>> holderCreator;

  public PEDeferredRegister(
      @NotNull ResourceKey<? extends Registry<T>> registryKey, @NotNull String namespace) {
    this(registryKey, namespace, PEDeferredHolder::new);
  }

  public PEDeferredRegister(
      @NotNull ResourceKey<? extends Registry<T>> registryKey,
      @NotNull String namespace,
      @NotNull Function<ResourceKey<T>, ? extends PEDeferredHolder<T, ? extends T>> holderCreator) {
    super(registryKey, namespace);
    this.holderCreator = holderCreator;
  }

  @NotNull
  public <I extends T> PEDeferredHolder<T, I> register(
      @NotNull String name, @NotNull Function<Identifier, ? extends I> func) {
    return (PEDeferredHolder<T, I>) super.<I>register(name, func);
  }

  @NotNull
  public <I extends T> PEDeferredHolder<T, I> register(
      @NotNull String name, @NotNull Supplier<? extends I> sup) {
    return (PEDeferredHolder<T, I>) super.<I>register(name, sup);
  }

  @NotNull
  protected <I extends T> PEDeferredHolder<T, I> createHolder(
      @NotNull ResourceKey<? extends Registry<T>> registryKey, @NotNull Identifier key) {
    return (PEDeferredHolder<T, I>) this.holderCreator.apply(ResourceKey.create(registryKey, key));
  }
}
