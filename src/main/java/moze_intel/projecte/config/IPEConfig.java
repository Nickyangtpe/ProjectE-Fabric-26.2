package moze_intel.projecte.config;

import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.config.value.CachedValue;

public interface IPEConfig {
  String getFileName();

  String getTranslation();

  ModConfigSpec getConfigSpec();

  default boolean isLoaded() {
    return this.getConfigSpec().isLoaded();
  }

  ModConfig.Type getConfigType();

  void save();

  void clearCache(boolean var1);

  <T> void addCachedValue(CachedValue<T> var1);
}
