package moze_intel.projecte.compat.network.handling;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.world.entity.player.Player;

public class ServerPayloadContext implements IPayloadContext {
  private final Context context;

  public ServerPayloadContext(Context context) {
    this.context = context;
  }

  @Override
  public Player player() {
    return this.context.player();
  }

  @Override
  public void enqueueWork(Runnable work) {
    this.context.server().execute(work);
  }
}
