package moze_intel.projecte.compat.network.client;

import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.compat.network.handling.IPayloadHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.entity.player.Player;

public final class ClientPayloadBridge {
  private ClientPayloadBridge() {}

  public static <PAYLOAD extends CustomPacketPayload> void registerReceiver(
      Type<PAYLOAD> type, IPayloadHandler<PAYLOAD> handler) {
    ClientPlayNetworking.registerGlobalReceiver(
        type,
        (payload, context) -> {
          IPayloadContext payloadContext = new ClientPayloadBridge.ClientPayloadContext(context);
          payloadContext.enqueueWork(() -> handler.handle((PAYLOAD) payload, payloadContext));
        });
  }

  private record ClientPayloadContext(Context context) implements IPayloadContext {
    @Override
    public Player player() {
      return this.context.player();
    }

    @Override
    public void enqueueWork(Runnable work) {
      this.context.client().execute(
          () -> {
            // Queued payloads from a disconnected server must not replace a new session's data.
            if (this.context.client().getConnection() == this.context.player().connection) {
              work.run();
            }
          });
    }
  }
}
