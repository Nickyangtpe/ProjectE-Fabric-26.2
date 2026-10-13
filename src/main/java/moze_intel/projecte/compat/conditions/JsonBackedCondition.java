package moze_intel.projecte.compat.conditions;

import com.google.gson.JsonElement;

public record JsonBackedCondition(JsonElement definition, String source) implements ICondition {
  @Override
  public boolean test() {
    return ConditionEvaluator.test(this.definition, this.source);
  }
}
