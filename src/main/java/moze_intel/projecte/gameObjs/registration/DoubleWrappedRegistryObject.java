package moze_intel.projecte.gameObjs.registration;

import moze_intel.projecte.compat.registries.DeferredHolder;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class DoubleWrappedRegistryObject<
        PRIMARY_REGISTRY,
        PRIMARY extends PRIMARY_REGISTRY,
        SECONDARY_REGISTRY,
        SECONDARY extends SECONDARY_REGISTRY>
    implements INamedEntry {
  protected final DeferredHolder<PRIMARY_REGISTRY, PRIMARY> primaryRO;
  protected final DeferredHolder<SECONDARY_REGISTRY, SECONDARY> secondaryRO;

  public DoubleWrappedRegistryObject(
      DeferredHolder<PRIMARY_REGISTRY, PRIMARY> primaryRO,
      DeferredHolder<SECONDARY_REGISTRY, SECONDARY> secondaryRO) {
    this.primaryRO = primaryRO;
    this.secondaryRO = secondaryRO;
  }

  @NotNull
  public PRIMARY getPrimary() {
    return this.primaryRO.get();
  }

  @NotNull
  public SECONDARY getSecondary() {
    return this.secondaryRO.get();
  }

  @Override
  public Identifier getId() {
    return this.primaryRO.getId();
  }

  @Override
  public String getName() {
    return this.primaryRO.getId().getPath();
  }
}
