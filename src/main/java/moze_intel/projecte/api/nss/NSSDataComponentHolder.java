package moze_intel.projecte.api.nss;

import net.minecraft.core.component.DataComponentPatch;
import org.jetbrains.annotations.NotNull;

public interface NSSDataComponentHolder extends NormalizedSimpleStack {
  @NotNull
  DataComponentPatch getComponentsPatch();

  default boolean hasModifiedData() {
    return !this.getComponentsPatch().isEmpty();
  }
}
