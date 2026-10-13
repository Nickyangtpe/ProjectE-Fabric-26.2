package moze_intel.projecte.config;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.fml.ModContainer;
import moze_intel.projecte.compat.fml.config.IConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.compat.fml.event.config.ModConfigEvent;
import moze_intel.projecte.compat.fml.loading.FMLPaths;

public final class ProjectEConfig {
  public static final Path CONFIG_DIR =
      FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve("ProjectE"));
  private static final Map<IConfigSpec, IPEConfig> KNOWN_CONFIGS = new HashMap<>();
  private static boolean registered;
  public static final ServerConfig server = new ServerConfig();
  public static final CommonConfig common = new CommonConfig();
  public static final ClientConfig client = new ClientConfig();

  private ProjectEConfig() {}

  public static synchronized void register(ModContainer modContainer) {
    if (!registered) {
      registerConfig(modContainer, server);
      registerConfig(modContainer, common);
      registerConfig(modContainer, client);
      registered = true;
    }
  }

  public static Collection<IPEConfig> getConfigs() {
    return Collections.unmodifiableCollection(KNOWN_CONFIGS.values());
  }

  public static synchronized void registerConfig(ModContainer modContainer, IPEConfig config) {
    if (!KNOWN_CONFIGS.containsKey(config.getConfigSpec())) {
      modContainer.registerConfig(
          config.getConfigType(),
          config.getConfigSpec(),
          "ProjectE/" + config.getFileName() + ".toml");
      KNOWN_CONFIGS.put(config.getConfigSpec(), config);
      ModConfig modConfig = modContainer.getConfig(config.getConfigSpec());
      if (modConfig == null) {
        throw new IllegalStateException(
            "Config registration did not create a config record for " + config.getFileName());
      }

      onConfigLoad(new ModConfigEvent.Loading(modConfig));
    }
  }

  public static synchronized void reload(ModContainer modContainer) {
    for (IPEConfig config : KNOWN_CONFIGS.values()) {
      ModConfig modConfig = modContainer.getConfig(config.getConfigSpec());
      if (modConfig != null) {
        modConfig.load();
        onConfigLoad(new ModConfigEvent.Reloading(modConfig));
      }
    }
  }

  public static synchronized void saveAll() {
    for (IPEConfig config : KNOWN_CONFIGS.values()) {
      if (config instanceof BasePEConfig baseConfig) {
        baseConfig.saveNow();
      } else {
        config.save();
      }
    }
  }

  public static void onConfigLoad(ModConfigEvent event) {
    ModConfig config = event.getConfig();
    if (config.getModId().equals("projecte")) {
      IPEConfig peConfig = KNOWN_CONFIGS.get(config.getSpec());
      if (peConfig != null) {
        peConfig.clearCache(event instanceof ModConfigEvent.Unloading);
      }

      PECore.postEvent(event);
    }
  }
}
