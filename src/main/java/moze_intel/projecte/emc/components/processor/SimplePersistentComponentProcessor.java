package moze_intel.projecte.emc.components.processor;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentPatch.Builder;
import org.jetbrains.annotations.NotNull;

public abstract class SimplePersistentComponentProcessor<TYPE> implements IDataComponentProcessor {
  protected abstract DataComponentType<TYPE> getComponentType(@NotNull ItemInfo var1);

  protected boolean validItem(@NotNull ItemInfo info) {
    return true;
  }

  protected abstract boolean shouldPersist(@NotNull ItemInfo var1, @NotNull TYPE var2);

  @Override
  public final boolean hasPersistentComponents() {
    return true;
  }

  protected TYPE cleanPersistentComponent(@NotNull TYPE component) {
    return component;
  }

  @Override
  public final void collectPersistentComponents(@NotNull ItemInfo info, @NotNull Builder builder) {
    if (this.validItem(info)) {
      DataComponentType<TYPE> componentType = this.getComponentType(info);
      TYPE component = info.getOrNull(componentType);
      if (component != null && this.shouldPersist(info, component)) {
        builder.set(componentType, this.cleanPersistentComponent(component));
      }
    }
  }
}
