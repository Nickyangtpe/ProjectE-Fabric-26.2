package moze_intel.projecte.gameObjs.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IFireProtector {
  default boolean canProtectAgainstFire(ItemStack stack, Player player) {
    return true;
  }
}
