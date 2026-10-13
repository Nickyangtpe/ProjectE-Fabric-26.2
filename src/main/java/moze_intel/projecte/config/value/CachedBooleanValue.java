package moze_intel.projecte.config.value;

import java.util.function.BooleanSupplier;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.IPEConfig;

public class CachedBooleanValue extends CachedValue<Boolean> implements BooleanSupplier {
  private boolean resolved;
  private boolean cachedValue;

  private CachedBooleanValue(IPEConfig config, ModConfigSpec.ConfigValue<Boolean> internal) {
    super(config, internal);
  }

  public static CachedBooleanValue wrap(
      IPEConfig config, ModConfigSpec.ConfigValue<Boolean> internal) {
    return new CachedBooleanValue(config, internal);
  }

  public boolean getOrDefault() {
    return !this.resolved && !this.isLoaded() ? this.internal.getDefault() : this.get();
  }

  public boolean get() {
    if (!this.resolved) {
      this.cachedValue = this.internal.get();
      this.resolved = true;
    }

    return this.cachedValue;
  }

  @Override
  public boolean getAsBoolean() {
    return this.get();
  }

  public void set(boolean value) {
    this.internal.set(value);
    this.cachedValue = value;
  }

  @Override
  protected boolean clearCachedValue(boolean checkChanged) {
    if (!this.resolved) {
      return false;
    }

    boolean oldCachedValue = this.cachedValue;
    this.resolved = false;
    return checkChanged && oldCachedValue != this.get();
  }
}
