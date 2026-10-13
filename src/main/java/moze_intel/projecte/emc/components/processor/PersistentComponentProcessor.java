package moze_intel.projecte.emc.components.processor;

import moze_intel.projecte.api.ItemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public abstract class PersistentComponentProcessor<TYPE>
    extends SimplePersistentComponentProcessor<TYPE> {
  protected abstract @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo var1, @Range(from = 1L, to = Long.MAX_VALUE) long var2, @NotNull TYPE var4)
      throws ArithmeticException;

  @Override
  public final @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info, @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC)
      throws ArithmeticException {
    if (this.validItem(info)) {
      TYPE component = info.getOrNull(this.getComponentType(info));
      if (component != null && this.shouldPersist(info, component)) {
        return this.recalculateEMC(info, currentEMC, component);
      }
    }

    return currentEMC;
  }
}
