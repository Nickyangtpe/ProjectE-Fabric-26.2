package moze_intel.projecte.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.value.CachedValue;

public abstract class BasePEConfig implements IPEConfig {
  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "ProjectE config saver");
            thread.setDaemon(true);
            return thread;
          });
  private final List<CachedValue<?>> cachedConfigValues = new ArrayList<>();

  @Override
  public synchronized void clearCache(boolean unloading) {
    for (CachedValue<?> cachedConfigValue : this.cachedConfigValues) {
      cachedConfigValue.clearCache(unloading);
    }
  }

  @Override
  public synchronized <T> void addCachedValue(CachedValue<T> configValue) {
    this.cachedConfigValues.add(configValue);
  }

  @Override
  public void save() {
    EXECUTOR.submit(new BasePEConfig.ConfigSaver(this.getConfigSpec()));
  }

  public void saveNow() {
    try {
      this.getConfigSpec().save();
    } catch (RuntimeException e) {
      PECore.LOGGER.error("Failed to save config", e);
    }
  }

  private static class ConfigSaver implements Runnable {
    private final ModConfigSpec configSpec;
    private int retries;

    private ConfigSaver(ModConfigSpec configSpec) {
      this.configSpec = configSpec;
    }

    @Override
    public void run() {
      try {
        this.configSpec.save();
      } catch (RuntimeException e) {
        PECore.LOGGER.error("Failed to save config", e);
        if (this.retries++ < 3) {
          BasePEConfig.EXECUTOR.submit(this);
        } else {
          PECore.LOGGER.error("Giving up on config save for {}", this.configSpec.getFile());
        }
      }
    }
  }
}
