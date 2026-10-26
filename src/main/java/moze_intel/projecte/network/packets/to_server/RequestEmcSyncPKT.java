package moze_intel.projecte.network.packets.to_server;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.network.LargeDataSync;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public record RequestEmcSyncPKT() implements IPEPacket {
  public static final RequestEmcSyncPKT INSTANCE = new RequestEmcSyncPKT();
  public static final Type<RequestEmcSyncPKT> TYPE = new Type<>(PECore.rl("request_emc_sync"));
  public static final StreamCodec<RegistryFriendlyByteBuf, RequestEmcSyncPKT> STREAM_CODEC =
      StreamCodec.unit(INSTANCE);

  @Override
  public Type<RequestEmcSyncPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    if (context.player() instanceof ServerPlayer player) {
      LargeDataSync.resend(player);
    }
  }
}
