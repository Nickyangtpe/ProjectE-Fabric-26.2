package moze_intel.projecte.compat.fml;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.compat.fml.loading.FMLPaths;

public final class ModContainer {
  private final String modId;
  private final Map<ModConfigSpec, ModConfig> configs = new IdentityHashMap<>();

  public ModContainer(String modId) {
    this.modId = Objects.requireNonNull(modId, "modId");
  }

  public String getModId() {
    return this.modId;
  }

  public synchronized void registerConfig(
      ModConfig.Type type, ModConfigSpec spec, String fileName) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(fileName, "fileName");
    if (this.configs.containsKey(spec)) {
      throw new IllegalStateException("This config spec is already registered for " + this.modId);
    }

    Path file = FMLPaths.CONFIGDIR.get().resolve(fileName);
    FMLPaths.getOrCreateGameRelativePath(file.getParent());
    ModConfig config = new ModConfig(this.modId, type, spec, file);
    config.load();
    this.configs.put(spec, config);
  }

  public synchronized ModConfig getConfig(ModConfig.Type type) {
    for (ModConfig config : this.configs.values()) {
      if (config.getType() == type) {
        return config;
      }
    }

    return null;
  }

  public synchronized ModConfig getConfig(ModConfigSpec spec) {
    return this.configs.get(spec);
  }

  public synchronized Collection<ModConfig> getConfigs() {
    return Collections.unmodifiableCollection(this.configs.values());
  }
}
