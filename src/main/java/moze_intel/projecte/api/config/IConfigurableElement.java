package moze_intel.projecte.api.config;

import moze_intel.projecte.compat.common.ModConfigSpec;

public interface IConfigurableElement {
  String getName();

  default String getConfigPath() {
    return this.getName().replace(' ', '-');
  }

  String getTranslationKey();

  String getDescription();

  default boolean isAvailable() {
    return true;
  }

  default void addConfigOptions(ModConfigSpec.Builder configBuilder) {}
}
