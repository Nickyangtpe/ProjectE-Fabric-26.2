package moze_intel.projecte.compat.conditions;

public record WithConditions<T>(T value, ICondition... conditions) {
  public boolean testConditions() {
    for (ICondition condition : this.conditions) {
      if (!condition.test()) {
        return false;
      }
    }

    return true;
  }
}
