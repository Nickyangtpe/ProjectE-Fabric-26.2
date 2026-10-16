package moze_intel.projecte.compat.conditions;

import com.mojang.serialization.MapCodec;

public interface ICondition {
  boolean test();

  default MapCodec<? extends ICondition> codec() {
    return null;
  }
}
