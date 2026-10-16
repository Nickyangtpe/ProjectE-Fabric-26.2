package moze_intel.projecte.emc.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.config.MappingConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.utils.AnnotationHelper;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPatch.Builder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class DataComponentManager {
  private static final List<IDataComponentProcessor> processors = new ArrayList<>();

  @Nullable
  private static volatile Supplier<? extends Iterable<RecipeHolder<?>>> clientRecipeHolderSupplier;

  public static void setClientRecipeHolderSupplier(
      @NotNull Supplier<? extends Iterable<RecipeHolder<?>>> supplier) {
    clientRecipeHolderSupplier = supplier;
  }

  @NotNull
  public static Iterable<RecipeHolder<?>> getRecipeHolders() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      return server.getRecipeManager().getRecipes();
    }

    Supplier<? extends Iterable<RecipeHolder<?>>> supplier = clientRecipeHolderSupplier;
    return supplier == null ? List.of() : supplier.get();
  }

  public static List<IDataComponentProcessor> loadProcessors() {
    if (processors.isEmpty()) {
      processors.addAll(AnnotationHelper.getDataComponentProcessors());
    }

    return Collections.unmodifiableList(processors);
  }

  public static void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    ComponentProcessorHelper.instance().updateCachedValues(emcLookup);

    for (IDataComponentProcessor processor : processors) {
      if (MappingConfig.isEnabled(processor)) {
        processor.updateCachedValues(emcLookup);
      }
    }
  }

  @NotNull
  static ItemInfo getPersistentInfo(@NotNull ItemInfo info) {
    if (info.hasModifiedComponents()
        && !info.getItem().is(PETags.Items.DATA_COMPONENT_WHITELIST)
        && !EMCMappingHandler.hasEmcValue(info)) {
      Builder builder = DataComponentPatch.builder();

      for (IDataComponentProcessor processor : processors) {
        if (MappingConfig.isEnabled(processor)
            && processor.hasPersistentComponents()
            && MappingConfig.hasPersistent(processor)) {
          processor.collectPersistentComponents(info, builder);
        }
      }

      return ItemInfo.fromItem(info.getItem(), builder.build());
    } else {
      return info;
    }
  }

  public static @Range(from = 0L, to = Long.MAX_VALUE) long getEmcValue(@NotNull ItemInfo info) {
    long emcValue = EMCMappingHandler.getStoredEmcValue(info);
    if (!info.hasModifiedComponents()) {
      return emcValue;
    }

    if (emcValue == 0L) {
      emcValue = EMCMappingHandler.getStoredEmcValue(info.itemOnly());
      if (emcValue == 0L) {
        return 0L;
      }
    }

    for (IDataComponentProcessor processor : processors) {
      if (MappingConfig.isEnabled(processor)) {
        try {
          emcValue = processor.recalculateEMC(info, emcValue);
        } catch (ArithmeticException e) {
          return 0L;
        }

        if (emcValue <= 0L) {
          return 0L;
        }
      }
    }

    return emcValue;
  }
}
