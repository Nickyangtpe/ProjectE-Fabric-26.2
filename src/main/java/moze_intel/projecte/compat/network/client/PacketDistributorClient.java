package moze_intel.projecte.compat.network.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class PacketDistributorClient {
  private PacketDistributorClient() {}

  public static void sendToServer(CustomPacketPayload payload) {
    ClientPlayNetworking.send(payload);
  }
}
