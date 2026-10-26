package moze_intel.projecte.network.packets.to_client;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.network.LargeDataSync;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record FragmentedSyncPKT(
    Identifier payloadType, long transferId, int totalBytes, int index, long checksum, byte[] data)
    implements IPEPacket {
  public static final Type<FragmentedSyncPKT> TYPE = new Type<>(PECore.rl("fragmented_sync"));
  public static final StreamCodec<RegistryFriendlyByteBuf, FragmentedSyncPKT> STREAM_CODEC =
      StreamCodec.composite(
          Identifier.STREAM_CODEC,
          FragmentedSyncPKT::payloadType,
          ByteBufCodecs.VAR_LONG,
          FragmentedSyncPKT::transferId,
          ByteBufCodecs.VAR_INT,
          FragmentedSyncPKT::totalBytes,
          ByteBufCodecs.VAR_INT,
          FragmentedSyncPKT::index,
          ByteBufCodecs.LONG,
          FragmentedSyncPKT::checksum,
          ByteBufCodecs.byteArray(LargeDataSync.CHUNK_BYTES),
          FragmentedSyncPKT::data,
          FragmentedSyncPKT::new);

  public FragmentedSyncPKT {
    if (transferId <= 0
        || totalBytes <= 0
        || totalBytes > LargeDataSync.MAX_TRANSFER_BYTES
        || index < 0
        || index >= (totalBytes - 1) / LargeDataSync.CHUNK_BYTES + 1
        || data.length
            != Math.min(
                LargeDataSync.CHUNK_BYTES, totalBytes - index * LargeDataSync.CHUNK_BYTES)) {
      throw new IllegalArgumentException("Invalid fragmented ProjectE snapshot");
    }
  }

  @Override
  public Type<FragmentedSyncPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    LargeDataSync.receive(this, context);
  }
}
