package moze_intel.projecte.compat.network.handling;

import net.minecraft.world.entity.player.Player;

public interface IPayloadContext {
  Player player();

  default void enqueueWork(Runnable work) {
    work.run();
  }
}
