package moze_intel.projecte.gameObjs.registration;

import moze_intel.projecte.compat.registries.DeferredHolder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class PEDeferredHolder<R, T extends R> extends DeferredHolder<R, T> implements INamedEntry {
  public PEDeferredHolder(ResourceKey<? extends Registry<R>> registryKey, Identifier valueName) {
    this(ResourceKey.create(registryKey, valueName));
  }

  public PEDeferredHolder(ResourceKey<R> key) {
    super(key);
  }

  @Override
  public String getName() {
    return this.getId().getPath();
  }
}
