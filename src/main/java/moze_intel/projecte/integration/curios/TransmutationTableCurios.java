package moze_intel.projecte.integration.curios;

import java.util.Optional;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.integration.trinkets.TrinketsCompat;
import net.minecraft.world.entity.player.Player;

public final class TransmutationTableCurios {
  private TransmutationTableCurios() {}

  public static Optional<IItemHandler> getCuriosInventory(Player player) {
    return !IntegrationHelper.isTrinketsLoaded()
        ? Optional.empty()
        : TrinketsCompat.getTabletInventory(player);
  }
}
