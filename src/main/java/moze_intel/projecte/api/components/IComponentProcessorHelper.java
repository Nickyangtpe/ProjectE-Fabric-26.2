package moze_intel.projecte.api.components;

import java.util.ServiceLoader;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface IComponentProcessorHelper {
  IComponentProcessorHelper INSTANCE =
      ServiceLoader.load(IComponentProcessorHelper.class)
          .findFirst()
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "No valid ServiceImpl for IComponentProcessorHelper found, ProjectE may be"
                          + " absent, damaged, or outdated"));

  @NotNull
  ItemInfo getPersistentInfo(@NotNull ItemInfo var1);

  @Range(from = 0L, to = Long.MAX_VALUE)
  long getMinEmcFor(@NotNull ToLongFunction<ItemInfo> var1, @NotNull Ingredient var2);

  @Range(from = 0L, to = Long.MAX_VALUE)
  long getColorEmc(@NotNull DyeColor var1);
}
