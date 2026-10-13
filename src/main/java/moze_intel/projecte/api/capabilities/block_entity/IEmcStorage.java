package moze_intel.projecte.api.capabilities.block_entity;

import org.jetbrains.annotations.Range;

public interface IEmcStorage {
  @Range(from = 0L, to = Long.MAX_VALUE)
  long getStoredEmc();

  @Range(from = 1L, to = Long.MAX_VALUE)
  long getMaximumEmc();

  default @Range(from = 0L, to = Long.MAX_VALUE) long getNeededEmc() {
    return Math.max(0L, this.getMaximumEmc() - this.getStoredEmc());
  }

  default boolean hasMaxedEmc() {
    return this.getStoredEmc() >= this.getMaximumEmc();
  }

  long extractEmc(long var1, IEmcStorage.EmcAction var3);

  long insertEmc(long var1, IEmcStorage.EmcAction var3);

  default boolean isRelay() {
    return false;
  }

  enum EmcAction {
    EXECUTE,
    SIMULATE;

    public boolean execute() {
      return this == EXECUTE;
    }

    public boolean simulate() {
      return this == SIMULATE;
    }

    public IEmcStorage.EmcAction combine(boolean execute) {
      return get(execute && this.execute());
    }

    public static IEmcStorage.EmcAction get(boolean execute) {
      return execute ? EXECUTE : SIMULATE;
    }
  }
}
