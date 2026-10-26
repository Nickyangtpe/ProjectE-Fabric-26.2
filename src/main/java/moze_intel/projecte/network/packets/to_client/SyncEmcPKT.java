package moze_intel.projecte.network.packets.to_client;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.network.LargeDataSync;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.NotNull;

public record SyncEmcPKT(Object2LongMap<ItemInfo> data) implements IPEPacket {
  public static final Type<SyncEmcPKT> TYPE = new Type(PECore.rl("sync_emc"));
  private static final StreamCodec<RegistryFriendlyByteBuf, Object2LongMap<ItemInfo>>
      MAP_STREAM_CODEC =
          ByteBufCodecs.map(
              Object2LongOpenHashMap::new, ItemInfo.STREAM_CODEC, ByteBufCodecs.VAR_LONG);
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncEmcPKT> STREAM_CODEC =
      MAP_STREAM_CODEC.map(SyncEmcPKT::new, SyncEmcPKT::data);

  @NotNull
  public Type<SyncEmcPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    PECore.debugLog("Receiving EMC data from server.");
    // An integrated server already owns the shared JVM map. A delayed client snapshot must
    // never roll that authoritative map back to an older generation.
    if (ServerLifecycleHooks.getCurrentServer() == null) {
      EMCMappingHandler.updateEmcValues(this.data);
    }
    LargeDataSync.receivedFullPayload(TYPE.id());
    if (context.player().containerMenu instanceof TransmutationContainer container) {
      try {
        container.transmutationInventory.updateClientTargets(false);
      } catch (RuntimeException exception) {
        PECore.LOGGER.error(
            "Could not refresh transmutation targets; the received EMC table is retained", exception);
      }
    }
  }

  public static SyncEmcPKT serializeEmcData(RegistryAccess registryAccess) {
    SyncEmcPKT data = EMCMappingHandler.createPacketData();
    RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);

    try {
      int index = buf.writerIndex();
      STREAM_CODEC.encode(buf, data);
      PECore.debugLog("EMC data size: {} bytes", buf.writerIndex() - index);
    } finally {
      buf.release();
    }

    return data;
  }
}
