package moze_intel.projecte.gameObjs.registration.impl;

import java.util.List;
import java.util.function.BooleanSupplier;
import moze_intel.projecte.compat.capabilities.BlockCapability;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class BlockEntityTypeRegistryObject<BE extends BlockEntity>
    extends PEDeferredHolder<BlockEntityType<?>, BlockEntityType<BE>> {
  @Nullable
  private List<BlockEntityTypeRegistryObject.CapabilityData<BE, ?, ?>> capabilityProviders;

  @Nullable private BlockEntityTicker<BE> clientTicker;
  @Nullable private BlockEntityTicker<BE> serverTicker;

  public BlockEntityTypeRegistryObject(ResourceKey<BlockEntityType<?>> key) {
    super(key);
  }

  @Nullable
  public BlockEntityTicker<BE> getTicker(boolean isClient) {
    return isClient ? this.clientTicker : this.serverTicker;
  }

  @Internal
  void tickers(
      @Nullable BlockEntityTicker<BE> clientTicker, @Nullable BlockEntityTicker<BE> serverTicker) {
    this.clientTicker = clientTicker;
    this.serverTicker = serverTicker;
  }

  @Internal
  void capabilities(
      @Nullable List<BlockEntityTypeRegistryObject.CapabilityData<BE, ?, ?>> capabilityProviders) {
    this.capabilityProviders = capabilityProviders;
  }

  @Internal
  void registerCapabilityProviders(RegisterCapabilitiesEvent event) {
    if (this.capabilityProviders != null) {
      for (BlockEntityTypeRegistryObject.CapabilityData<BE, ?, ?> capabilityProvider :
          this.capabilityProviders) {
        capabilityProvider.registerProvider(event, this.get());
      }
    }
  }

  @Internal
  record CapabilityData<BE extends BlockEntity, CAP, CONTEXT>(
      BlockCapability<CAP, CONTEXT> capability,
      ICapabilityProvider<? super BE, CONTEXT, CAP> provider,
      BooleanSupplier shouldApply) {
    private void registerProvider(RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
      if (this.shouldApply.getAsBoolean()) {
        event.registerBlockEntity(
            this.capability,
            type,
            (blockEntity, context) -> this.provider.getCapability((BE) blockEntity, context));
      }
    }
  }
}
