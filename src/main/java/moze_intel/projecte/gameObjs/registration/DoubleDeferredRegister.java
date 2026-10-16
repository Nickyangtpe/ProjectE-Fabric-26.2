package moze_intel.projecte.gameObjs.registration;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.compat.eventbus.IEventBus;
import moze_intel.projecte.compat.registries.DeferredHolder;
import moze_intel.projecte.compat.registries.DeferredRegister;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

public class DoubleDeferredRegister<PRIMARY, SECONDARY> {
  @NotNull protected final DeferredRegister<PRIMARY> primaryRegister;
  @NotNull protected final DeferredRegister<SECONDARY> secondaryRegister;

  public DoubleDeferredRegister(
      @NotNull DeferredRegister<PRIMARY> primaryRegistry,
      @NotNull DeferredRegister<SECONDARY> secondaryRegistry) {
    this.primaryRegister = primaryRegistry;
    this.secondaryRegister = secondaryRegistry;
  }

  protected DoubleDeferredRegister(
      ResourceKey<? extends Registry<PRIMARY>> primaryRegistryName,
      ResourceKey<? extends Registry<SECONDARY>> secondaryRegistryName,
      String modid) {
    this(primaryRegistryName, PEDeferredRegister.create(secondaryRegistryName, modid), modid);
  }

  protected DoubleDeferredRegister(
      ResourceKey<? extends Registry<PRIMARY>> primaryRegistryName,
      DeferredRegister<SECONDARY> secondaryRegistry,
      String modid) {
    this(PEDeferredRegister.create(primaryRegistryName, modid), secondaryRegistry);
  }

  public <
          P extends PRIMARY,
          S extends SECONDARY,
          W extends DoubleWrappedRegistryObject<PRIMARY, P, SECONDARY, S>>
      W register(
          String name,
          Supplier<? extends P> primarySupplier,
          Supplier<? extends S> secondarySupplier,
          BiFunction<DeferredHolder<PRIMARY, P>, DeferredHolder<SECONDARY, S>, W> objectWrapper) {
    return objectWrapper.apply(
        this.primaryRegister.register(name, primarySupplier),
        this.secondaryRegister.register(name, secondarySupplier));
  }

  public <
          P extends PRIMARY,
          S extends SECONDARY,
          W extends DoubleWrappedRegistryObject<PRIMARY, P, SECONDARY, S>>
      W register(
          String name,
          Supplier<? extends P> primarySupplier,
          Function<P, S> secondarySupplier,
          BiFunction<DeferredHolder<PRIMARY, P>, DeferredHolder<SECONDARY, S>, W> objectWrapper) {
    return this.registerAdvanced(
        name,
        primarySupplier,
        primaryObject -> secondarySupplier.apply(primaryObject.get()),
        objectWrapper);
  }

  public <
          P extends PRIMARY,
          S extends SECONDARY,
          W extends DoubleWrappedRegistryObject<PRIMARY, P, SECONDARY, S>>
      W registerAdvanced(
          String name,
          Supplier<? extends P> primarySupplier,
          Function<DeferredHolder<PRIMARY, P>, S> secondarySupplier,
          BiFunction<DeferredHolder<PRIMARY, P>, DeferredHolder<SECONDARY, S>, W> objectWrapper) {
    DeferredHolder<PRIMARY, P> primaryObject = this.primaryRegister.register(name, primarySupplier);
    return objectWrapper.apply(
        primaryObject,
        this.secondaryRegister.register(name, () -> secondarySupplier.apply(primaryObject)));
  }

  public void register(IEventBus bus) {
    this.primaryRegister.register(bus);
    this.secondaryRegister.register(bus);
  }

  public Collection<DeferredHolder<PRIMARY, ? extends PRIMARY>> getPrimaryEntries() {
    return this.primaryRegister.getEntries();
  }

  public Collection<DeferredHolder<SECONDARY, ? extends SECONDARY>> getSecondaryEntries() {
    return this.secondaryRegister.getEntries();
  }
}
