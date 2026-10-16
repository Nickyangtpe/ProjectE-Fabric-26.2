package moze_intel.projecte.config.value;

import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.IPEConfig;

public class CachedFloatValue extends CachedValue<Double> {
  private boolean resolved;
  private float cachedValue;

  private CachedFloatValue(IPEConfig config, ModConfigSpec.ConfigValue<Double> internal) {
    super(config, internal);
  }

  public static CachedFloatValue wrap(
      IPEConfig config, ModConfigSpec.ConfigValue<Double> internal) {
    return new CachedFloatValue(config, internal);
  }

  public float getOrDefault() {
    return !this.resolved && !this.isLoaded()
        ? this.clampInternal(this.internal.getDefault())
        : this.get();
  }

  public float get() {
    if (!this.resolved) {
      this.cachedValue = this.clampInternal(this.internal.get());
      this.resolved = true;
    }

    return this.cachedValue;
  }

  private float clampInternal(Double val) {
    if (val == null) {
      return 0.0F;
    } else if (val > Float.MAX_VALUE) {
      return Float.MAX_VALUE;
    } else {
      return val < -Float.MAX_VALUE ? -Float.MAX_VALUE : val.floatValue();
    }
  }

  public void set(float value) {
    this.internal.set((double) value);
    this.cachedValue = value;
  }

  @Override
  protected boolean clearCachedValue(boolean checkChanged) {
    if (!this.resolved) {
      return false;
    }

    float oldCachedValue = this.cachedValue;
    this.resolved = false;
    return checkChanged && oldCachedValue != this.get();
  }
}
