package moze_intel.projecte.config.value;

import java.util.function.IntSupplier;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.IPEConfig;

public class CachedIntValue extends CachedValue<Integer> implements IntSupplier {
  private boolean resolved;
  private int cachedValue;

  private CachedIntValue(IPEConfig config, ModConfigSpec.ConfigValue<Integer> internal) {
    super(config, internal);
  }

  public static CachedIntValue wrap(IPEConfig config, ModConfigSpec.ConfigValue<Integer> internal) {
    return new CachedIntValue(config, internal);
  }

  public int getOrDefault() {
    return !this.resolved && !this.isLoaded() ? this.internal.getDefault() : this.get();
  }

  public int get() {
    if (!this.resolved) {
      this.cachedValue = this.internal.get();
      this.resolved = true;
    }

    return this.cachedValue;
  }

  @Override
  public int getAsInt() {
    return this.get();
  }

  public void set(int value) {
    this.internal.set(value);
    this.cachedValue = value;
  }

  @Override
  protected boolean clearCachedValue(boolean checkChanged) {
    if (!this.resolved) {
      return false;
    }

    int oldCachedValue = this.cachedValue;
    this.resolved = false;
    return checkChanged && oldCachedValue != this.get();
  }
}
