package moze_intel.projecte.gameObjs.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import org.jetbrains.annotations.NotNull;

public class PhilosStoneContainer extends CraftingMenu {
  public PhilosStoneContainer(
      int windowId, Inventory invPlayer, ContainerLevelAccess worldPosCallable) {
    super(windowId, invPlayer, worldPosCallable);
  }

  public boolean stillValid(@NotNull Player player) {
    return true;
  }
}
