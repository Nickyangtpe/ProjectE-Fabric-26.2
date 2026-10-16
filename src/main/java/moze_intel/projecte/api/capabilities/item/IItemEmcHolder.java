package moze_intel.projecte.api.capabilities.item;

import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface IItemEmcHolder {
  long insertEmc(@NotNull ItemStack var1, long var2, IEmcStorage.EmcAction var4);

  long extractEmc(@NotNull ItemStack var1, long var2, IEmcStorage.EmcAction var4);

  @Range(from = 0L, to = Long.MAX_VALUE)
  long getStoredEmc(@NotNull ItemStack var1);

  @Range(from = 1L, to = Long.MAX_VALUE)
  long getMaximumEmc(@NotNull ItemStack var1);

  default @Range(from = 0L, to = Long.MAX_VALUE) long getNeededEmc(@NotNull ItemStack stack) {
    return Math.max(0L, this.getMaximumEmc(stack) - this.getStoredEmc(stack));
  }
}
