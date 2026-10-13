package moze_intel.projecte.api.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public interface ITransmutationTablet {
  void openContainer(Player var1, InteractionHand var2, int var3);

  void openContainer(Player var1);
}
