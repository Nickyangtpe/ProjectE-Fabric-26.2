package moze_intel.projecte.api.components;

import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.config.IConfigurableElement;
import net.minecraft.core.component.DataComponentPatch.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface IDataComponentProcessor extends IConfigurableElement {
  @Override
  default boolean isAvailable() {
    return IConfigurableElement.super.isAvailable();
  }

  default boolean hasPersistentComponents() {
    return false;
  }

  default boolean usePersistentComponents() {
    return this.hasPersistentComponents();
  }

  @Range(from = 0L, to = Long.MAX_VALUE)
  long recalculateEMC(@NotNull ItemInfo var1, @Range(from = 1L, to = Long.MAX_VALUE) long var2)
      throws ArithmeticException;

  default void collectPersistentComponents(@NotNull ItemInfo info, @NotNull Builder builder) {}

  default void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {}
}
