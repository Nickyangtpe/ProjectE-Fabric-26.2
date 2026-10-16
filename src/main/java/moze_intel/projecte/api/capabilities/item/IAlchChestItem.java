package moze_intel.projecte.api.capabilities.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface IAlchChestItem {
  boolean updateInAlchChest(@NotNull Level var1, @NotNull BlockPos var2, @NotNull ItemStack var3);
}
