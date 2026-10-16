package moze_intel.projecte.compat.network;

import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.compat.network.client.PacketDistributorClient;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PacketDistributor {
  private PacketDistributor() {}

  public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
    ServerPlayNetworking.send(player, payload);
  }

  public static void sendToServer(CustomPacketPayload payload) {
    if (!FMLEnvironment.dist.isClient()) {
      throw new IllegalStateException(
          "Cannot send a payload to the server from the dedicated server.");
    }

    PacketDistributorClient.sendToServer(payload);
  }
}
