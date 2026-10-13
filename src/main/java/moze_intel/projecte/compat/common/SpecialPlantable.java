package moze_intel.projecte.compat.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface SpecialPlantable {
  boolean canPlacePlantAtPosition(ItemStack var1, Level var2, BlockPos var3, Direction var4);

  void spawnPlantAtPosition(ItemStack var1, Level var2, BlockPos var3, Direction var4);
}
