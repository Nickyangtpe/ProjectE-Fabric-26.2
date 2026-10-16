package moze_intel.projecte.gameObjs.registration.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.compat.capabilities.BlockCapability;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.compat.eventbus.IEventBus;
import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.compat.registries.DeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockEntityTypeDeferredRegister extends PEDeferredRegister<BlockEntityType<?>> {
  public BlockEntityTypeDeferredRegister(String modid) {
    super(Registries.BLOCK_ENTITY_TYPE, modid, BlockEntityTypeRegistryObject::new);
  }

  public <BE extends BlockEntity>
      BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> builder(
          BlockRegistryObject<?, ?> block, BlockEntitySupplier<? extends BE> factory) {
    return new BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<>(block, factory);
  }

  private <BE extends BlockEntity> BlockEntityTypeRegistryObject<BE> registerPE(
      String name, Supplier<? extends BlockEntityType<BE>> sup) {
    return (BlockEntityTypeRegistryObject<BE>) super.<BlockEntityType<BE>>register(name, sup);
  }

  @Override
  public void register(@NotNull IEventBus bus) {
    super.register(bus);
    bus.addListener(this::registerCapabilities);
  }

  private void registerCapabilities(RegisterCapabilitiesEvent event) {
    for (DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> entry :
        this.getEntries()) {
      if (entry instanceof BlockEntityTypeRegistryObject<?> beRO) {
        beRO.registerCapabilityProviders(event);
      } else if (!FMLEnvironment.production) {
        throw new IllegalStateException("Expected entry to be a BlockEntityTypeRegistryObject");
      }
    }
  }

  public class BlockEntityTypeBuilder<BE extends BlockEntity> {
    private static final ICapabilityProvider<?, ?, ?> SIMPLE_PROVIDER = (obj, context) -> obj;
    private final BlockRegistryObject<?, ?> block;
    private final BlockEntitySupplier<? extends BE> factory;
    private final List<BlockEntityTypeRegistryObject.CapabilityData<BE, ?, ?>> capabilityProviders =
        new ArrayList<>();
    @Nullable private BlockEntityTicker<BE> clientTicker;
    @Nullable private BlockEntityTicker<BE> serverTicker;

    BlockEntityTypeBuilder(
        BlockRegistryObject<?, ?> block, BlockEntitySupplier<? extends BE> factory) {
      this.block = block;
      this.factory = factory;
    }

    public <CAP, CONTEXT> BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> withSimple(
        BlockCapability<CAP, CONTEXT> capability) {
      return this.withSimple(capability, () -> true);
    }

    public <CAP, CONTEXT> BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> withSimple(
        BlockCapability<CAP, CONTEXT> capability, BooleanSupplier shouldApply) {
      return this.with(
          capability, (ICapabilityProvider<? super BE, CONTEXT, CAP>) SIMPLE_PROVIDER, shouldApply);
    }

    public <CAP, CONTEXT> BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> with(
        BlockCapability<CAP, CONTEXT> capability,
        Function<BlockCapability<CAP, CONTEXT>, ICapabilityProvider<? super BE, CONTEXT, CAP>>
            provider) {
      return this.with(capability, provider.apply(capability));
    }

    public <CAP, CONTEXT> BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> with(
        BlockCapability<CAP, CONTEXT> capability,
        ICapabilityProvider<? super BE, CONTEXT, CAP> provider) {
      return this.with(capability, provider, () -> true);
    }

    public <CAP, CONTEXT> BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> with(
        BlockCapability<CAP, CONTEXT> capability,
        ICapabilityProvider<? super BE, CONTEXT, CAP> provider,
        BooleanSupplier shouldApply) {
      this.capabilityProviders.add(
          new BlockEntityTypeRegistryObject.CapabilityData<>(capability, provider, shouldApply));
      return this;
    }

    public BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> without(
        BlockCapability<?, ?>... capabilities) {
      for (BlockCapability<?, ?> capability : capabilities) {
        this.capabilityProviders.removeIf(data -> data.capability() == capability);
      }

      return this;
    }

    public BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> without(
        Collection<? extends BlockCapability<?, ?>> capabilities) {
      this.capabilityProviders.removeIf(data -> capabilities.contains(data.capability()));
      return this;
    }

    public BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> clientTicker(
        BlockEntityTicker<BE> ticker) {
      Preconditions.checkState(this.clientTicker == null, "Client ticker may only be set once.");
      this.clientTicker = ticker;
      return this;
    }

    public BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> serverTicker(
        BlockEntityTicker<BE> ticker) {
      Preconditions.checkState(this.serverTicker == null, "Server ticker may only be set once.");
      this.serverTicker = ticker;
      return this;
    }

    public BlockEntityTypeDeferredRegister.BlockEntityTypeBuilder<BE> commonTicker(
        BlockEntityTicker<BE> ticker) {
      return this.clientTicker(ticker).serverTicker(ticker);
    }

    public BlockEntityTypeRegistryObject<BE> build() {
      BlockEntityTypeRegistryObject<BE> holder =
          BlockEntityTypeDeferredRegister.this.registerPE(
              this.block.getName(),
              () -> new BlockEntityType(this.factory, Set.of(this.block.getBlocks())));
      holder.tickers(this.clientTicker, this.serverTicker);
      holder.capabilities(this.capabilityProviders.isEmpty() ? null : this.capabilityProviders);
      return holder;
    }
  }
}
