package moze_intel.projecte.config.value;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.IPEConfig;

public abstract class CachedValue<T> {
  private final IPEConfig config;
  protected final ModConfigSpec.ConfigValue<T> internal;
  private Set<CachedValue.IConfigValueInvalidationListener> invalidationListeners;

  protected CachedValue(IPEConfig config, ModConfigSpec.ConfigValue<T> internal) {
    this.config = config;
    this.internal = internal;
    this.config.addCachedValue(this);
  }

  public boolean hasInvalidationListeners() {
    return this.invalidationListeners != null && !this.invalidationListeners.isEmpty();
  }

  public void addInvalidationListener(CachedValue.IConfigValueInvalidationListener listener) {
    if (this.invalidationListeners == null) {
      this.invalidationListeners = new HashSet<>();
    }

    if (!this.invalidationListeners.add(listener)) {
      PECore.LOGGER.warn("Duplicate invalidation listener added");
    }
  }

  public void removeInvalidationListener(CachedValue.IConfigValueInvalidationListener listener) {
    if (this.invalidationListeners == null) {
      PECore.LOGGER.warn(
          "Unable to remove specified invalidation listener, no invalidation listeners have been"
              + " added.");
    } else if (!this.invalidationListeners.remove(listener)) {
      PECore.LOGGER.warn("Unable to remove specified invalidation listener.");
    }
  }

  public boolean removeInvalidationListenersMatching(
      Predicate<CachedValue.IConfigValueInvalidationListener> checker) {
    return this.invalidationListeners != null
        && !this.invalidationListeners.isEmpty()
        && this.invalidationListeners.removeIf(checker);
  }

  protected abstract boolean clearCachedValue(boolean var1);

  public final void clearCache(boolean unloading) {
    if (this.hasInvalidationListeners()) {
      if (!unloading && this.isLoaded() && this.clearCachedValue(true)) {
        this.invalidationListeners.forEach(Runnable::run);
      }
    } else {
      this.clearCachedValue(false);
    }
  }

  protected boolean isLoaded() {
    return this.config.isLoaded();
  }

  @FunctionalInterface
  public interface IConfigValueInvalidationListener extends Runnable {}
}
