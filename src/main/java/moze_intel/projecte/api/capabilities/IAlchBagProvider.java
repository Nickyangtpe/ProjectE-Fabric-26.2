package moze_intel.projecte.api.capabilities;

import java.util.Set;
import moze_intel.projecte.compat.items.IItemHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

public interface IAlchBagProvider {
  @NotNull
  IItemHandler getBag(@NotNull DyeColor var1);

  void sync(@NotNull ServerPlayer var1, @NotNull Set<DyeColor> var2);

  void syncAllBags(@NotNull ServerPlayer var1);
}
