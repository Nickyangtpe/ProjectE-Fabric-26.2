package moze_intel.projecte.compat.common;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IShearable {
  boolean isShearable(Player var1, ItemStack var2, Level var3, BlockPos var4);

  List<ItemStack> onSheared(Player var1, ItemStack var2, Level var3, BlockPos var4);

  default void spawnShearedDrop(Level level, BlockPos pos, ItemStack drop) {
    if (!level.isClientSide() && !drop.isEmpty()) {
      ItemEntity itemEntity =
          new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
      itemEntity.setDefaultPickUpDelay();
      level.addFreshEntity(itemEntity);
    }
  }
}
