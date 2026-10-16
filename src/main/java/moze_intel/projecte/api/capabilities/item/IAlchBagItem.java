package moze_intel.projecte.api.capabilities.item;

import moze_intel.projecte.compat.items.IItemHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IAlchBagItem {
  boolean updateInAlchBag(
      @NotNull IItemHandler var1, @NotNull Player var2, @NotNull ItemStack var3);
}
