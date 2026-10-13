package moze_intel.projecte.emc.components.processor;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public abstract class SimpleContainerProcessor<TYPE> extends PersistentComponentProcessor<TYPE> {
  @Override
  public boolean usePersistentComponents() {
    return false;
  }

  protected abstract Iterable<ItemStack> getStoredItems(TYPE var1);

  @Override
  protected final @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info,
      @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC,
      @NotNull TYPE component)
      throws ArithmeticException {
    for (ItemStack item : this.getStoredItems(component)) {
      if (!item.isEmpty()) {
        long itemEmc = IEMCProxy.INSTANCE.getValue(item);
        if (itemEmc == 0L) {
          return 0L;
        }

        long stackEmc = Math.multiplyExact(itemEmc, item.getCount());
        currentEMC = Math.addExact(currentEMC, stackEmc);
      }
    }

    return currentEMC;
  }
}
