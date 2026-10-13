package moze_intel.projecte.api.mapper;

import moze_intel.projecte.api.config.IConfigurableElement;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;

public interface IEMCMapper<T, V extends Comparable<V>> extends IConfigurableElement {
  @Override
  default boolean isAvailable() {
    return IConfigurableElement.super.isAvailable();
  }

  void addMappings(
      IMappingCollector<T, V> var1,
      ReloadableServerResources var2,
      RegistryAccess var3,
      ResourceManager var4);
}
