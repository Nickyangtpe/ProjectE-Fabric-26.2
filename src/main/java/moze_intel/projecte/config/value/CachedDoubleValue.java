package moze_intel.projecte.config.value;

import java.util.function.DoubleSupplier;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.IPEConfig;

public class CachedDoubleValue extends CachedValue<Double> implements DoubleSupplier {
  private boolean resolved;
  private double cachedValue;

  private CachedDoubleValue(IPEConfig config, ModConfigSpec.ConfigValue<Double> internal) {
    super(config, internal);
  }

  public static CachedDoubleValue wrap(
      IPEConfig config, ModConfigSpec.ConfigValue<Double> internal) {
    return new CachedDoubleValue(config, internal);
  }

  public double getOrDefault() {
    return !this.resolved && !this.isLoaded() ? this.internal.getDefault() : this.get();
  }

  public double get() {
    if (!this.resolved) {
      this.cachedValue = this.internal.get();
      this.resolved = true;
    }

    return this.cachedValue;
  }

  @Override
  public double getAsDouble() {
    return this.get();
  }

  public void set(double value) {
    this.internal.set(value);
    this.cachedValue = value;
  }

  @Override
  protected boolean clearCachedValue(boolean checkChanged) {
    if (!this.resolved) {
      return false;
    }

    double oldCachedValue = this.cachedValue;
    this.resolved = false;
    return checkChanged && oldCachedValue != this.get();
  }
}
