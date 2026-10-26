package moze_intel.projecte.network;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.FragmentedSyncPKT;
import moze_intel.projecte.network.packets.to_client.SyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncPKT;
import moze_intel.projecte.network.packets.to_server.RequestEmcSyncPKT;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Byte-bounded transport for growing EMC maps and player knowledge snapshots. */
public final class LargeDataSync {
  public static final int CHUNK_BYTES = 32 * 1024;
  public static final int MAX_TRANSFER_BYTES = 64 * 1024 * 1024;
  private static final long TRANSFER_TIMEOUT_NANOS = 60_000_000_000L;
  private static final long RETRY_INTERVAL_NANOS = 6_000_000_000L;
  private static final long REQUEST_THROTTLE_NANOS = 5_000_000_000L;
  private static final AtomicLong NEXT_TRANSFER_ID = new AtomicLong();
  private static final Map<Identifier, PendingTransfer> CLIENT_TRANSFERS = new HashMap<>();
  private static final Map<Identifier, Long> COMPLETED_TRANSFERS = new HashMap<>();
  private static final Set<Identifier> AWAITING_RESPONSE = new HashSet<>();
  private static final Map<ServerPlayer, Long> LAST_REQUESTS = new WeakHashMap<>();
  @Nullable private static Object2LongMap<ItemInfo> cachedEmcData;
  @Nullable private static RegistryAccess cachedRegistries;
  private static List<FragmentedSyncPKT> cachedEmcFragments = List.of();
  private static boolean clientRetryRequested;
  private static long lastClientRetry;

  private LargeDataSync() {}

  /** Returns false for payloads which do not need this transport. */
  public static boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
    if (!(payload instanceof SyncEmcPKT) && !(payload instanceof KnowledgeSyncPKT)) {
      return false;
    }

    List<FragmentedSyncPKT> fragments;
    RegistryAccess registries = player.level().registryAccess();
    if (payload instanceof SyncEmcPKT emcPacket) {
      if (cachedEmcData != emcPacket.data() || cachedRegistries != registries) {
        fragments = fragment(payload.type().id(), encode(registries, payload));
        cachedEmcData = emcPacket.data();
        cachedRegistries = registries;
        cachedEmcFragments = fragments;
      } else {
        fragments = cachedEmcFragments;
      }
    } else {
      fragments = fragment(payload.type().id(), encode(registries, payload));
    }

    if (fragments.isEmpty()) {
      // Retain the original wire format for normal-sized maps and older clients.
      ServerPlayNetworking.send(player, payload);
    } else {
      if (!ServerPlayNetworking.canSend(player, FragmentedSyncPKT.TYPE)) {
        throw new IllegalStateException(
            "Client cannot receive large ProjectE data; update ProjectE on both client and server");
      }
      for (FragmentedSyncPKT fragment : fragments) {
        ServerPlayNetworking.send(player, fragment);
      }
      PECore.debugLog(
          "Sent {} in {} fragments ({} bytes) to {}",
          payload.type().id(),
          fragments.size(),
          fragments.getFirst().totalBytes(),
          player.getName().getString());
    }
    return true;
  }

  private static byte[] encode(RegistryAccess registries, CustomPacketPayload payload) {
    RegistryFriendlyByteBuf buffer =
        new RegistryFriendlyByteBuf(Unpooled.buffer(256, MAX_TRANSFER_BYTES), registries);
    try {
      if (payload instanceof SyncEmcPKT packet) {
        SyncEmcPKT.STREAM_CODEC.encode(buffer, packet);
      } else if (payload instanceof KnowledgeSyncPKT packet) {
        KnowledgeSyncPKT.STREAM_CODEC.encode(buffer, packet);
      } else {
        throw new IllegalArgumentException("Unsupported fragmented payload " + payload.type().id());
      }
      byte[] bytes = new byte[buffer.readableBytes()];
      buffer.readBytes(bytes);
      return bytes;
    } finally {
      buffer.release();
    }
  }

  private static List<FragmentedSyncPKT> fragment(Identifier type, byte[] bytes) {
    if (bytes.length <= CHUNK_BYTES) {
      return List.of();
    }
    CRC32 checksum = new CRC32();
    checksum.update(bytes);
    long transferId = NEXT_TRANSFER_ID.incrementAndGet();
    List<FragmentedSyncPKT> fragments = new ArrayList<>();
    for (int offset = 0, index = 0; offset < bytes.length; offset += CHUNK_BYTES, index++) {
      fragments.add(
          new FragmentedSyncPKT(
              type,
              transferId,
              bytes.length,
              index,
              checksum.getValue(),
              Arrays.copyOfRange(bytes, offset, Math.min(bytes.length, offset + CHUNK_BYTES))));
    }
    return List.copyOf(fragments);
  }

  public static void receive(FragmentedSyncPKT packet, IPayloadContext context) {
    Identifier type = packet.payloadType();
    if (!type.equals(SyncEmcPKT.TYPE.id()) && !type.equals(KnowledgeSyncPKT.TYPE.id())) {
      throw new IllegalArgumentException("Unsupported fragmented payload " + type);
    }
    if (packet.transferId() <= COMPLETED_TRANSFERS.getOrDefault(type, 0L)) {
      AWAITING_RESPONSE.remove(type);
      return;
    }
    PendingTransfer pending = CLIENT_TRANSFERS.get(type);
    if (pending != null && packet.transferId() < pending.transferId) {
      return;
    }

    try {
      if (pending == null || pending.transferId != packet.transferId()) {
        pending = new PendingTransfer(packet);
        CLIENT_TRANSFERS.put(type, pending);
      }
      if (!pending.accept(packet)) {
        return;
      }

      CRC32 checksum = new CRC32();
      checksum.update(pending.bytes);
      if (checksum.getValue() != pending.checksum) {
        throw new IllegalArgumentException("Checksum mismatch for " + type);
      }
      RegistryFriendlyByteBuf buffer =
          new RegistryFriendlyByteBuf(
              Unpooled.wrappedBuffer(pending.bytes), context.player().level().registryAccess());
      IPEPacket complete;
      try {
        complete =
            type.equals(SyncEmcPKT.TYPE.id())
                ? SyncEmcPKT.STREAM_CODEC.decode(buffer)
                : KnowledgeSyncPKT.STREAM_CODEC.decode(buffer);
        if (buffer.isReadable()) {
          throw new IllegalArgumentException("Unexpected trailing data for " + type);
        }
      } finally {
        buffer.release();
      }
      // No partial map or knowledge snapshot ever reaches the active player state.
      complete.handle(context);
      COMPLETED_TRANSFERS.put(type, packet.transferId());
      CLIENT_TRANSFERS.remove(type);
    } catch (RuntimeException exception) {
      CLIENT_TRANSFERS.remove(type);
      clientRetryRequested = true;
      PECore.LOGGER.error(
          "Could not receive {}. Keeping the previous data and requesting a fresh snapshot",
          type,
          exception);
    }
  }

  public static void receivedFullPayload(Identifier type) {
    CLIENT_TRANSFERS.remove(type);
    AWAITING_RESPONSE.remove(type);
  }

  public static void requestClientSync() {
    clientRetryRequested = true;
  }

  public static void tickClient() {
    long now = System.nanoTime();
    Iterator<Map.Entry<Identifier, PendingTransfer>> iterator =
        CLIENT_TRANSFERS.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Identifier, PendingTransfer> entry = iterator.next();
      if (now - entry.getValue().lastReceived > TRANSFER_TIMEOUT_NANOS) {
        PECore.LOGGER.warn(
            "Timed out receiving {}; requesting EMC and knowledge again", entry.getKey());
        iterator.remove();
        clientRetryRequested = true;
      }
    }
    if (!AWAITING_RESPONSE.isEmpty()
        && CLIENT_TRANSFERS.isEmpty()
        && now - lastClientRetry >= RETRY_INTERVAL_NANOS) {
      clientRetryRequested = true;
    }
    if (clientRetryRequested
        && (lastClientRetry == 0 || now - lastClientRetry >= RETRY_INTERVAL_NANOS)) {
      lastClientRetry = now;
      clientRetryRequested = false;
      try {
        AWAITING_RESPONSE.add(SyncEmcPKT.TYPE.id());
        AWAITING_RESPONSE.add(KnowledgeSyncPKT.TYPE.id());
        PacketDistributor.sendToServer(RequestEmcSyncPKT.INSTANCE);
      } catch (RuntimeException exception) {
        clientRetryRequested = true;
        PECore.debugLog("Could not request EMC synchronization yet: {}", exception.toString());
      }
    }
  }

  public static void resend(ServerPlayer player) {
    long now = System.nanoTime();
    Long previous = LAST_REQUESTS.get(player);
    if (previous != null && now - previous < REQUEST_THROTTLE_NANOS) {
      return;
    }
    LAST_REQUESTS.put(player, now);
    if (EMCMappingHandler.isAvailable()) {
      PacketDistributor.sendToPlayer(player, EMCMappingHandler.createPacketData());
      PacketDistributor.sendToPlayer(player, FuelMapper.getSyncPacket());
    }
    IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    if (knowledge != null) {
      knowledge.sync(player);
    }
  }

  public static void resetClient() {
    CLIENT_TRANSFERS.clear();
    COMPLETED_TRANSFERS.clear();
    AWAITING_RESPONSE.clear();
    clientRetryRequested = false;
    lastClientRetry = 0;
  }

  public static void resetServer() {
    cachedEmcData = null;
    cachedRegistries = null;
    cachedEmcFragments = List.of();
    LAST_REQUESTS.clear();
  }

  private static final class PendingTransfer {
    private final long transferId;
    private final long checksum;
    private final byte[] bytes;
    private final BitSet received = new BitSet();
    private final int parts;
    private long lastReceived;

    private PendingTransfer(FragmentedSyncPKT packet) {
      this.transferId = packet.transferId();
      this.checksum = packet.checksum();
      this.bytes = new byte[packet.totalBytes()];
      this.parts = (packet.totalBytes() - 1) / CHUNK_BYTES + 1;
    }

    private boolean accept(FragmentedSyncPKT packet) {
      if (packet.totalBytes() != this.bytes.length || packet.checksum() != this.checksum) {
        throw new IllegalArgumentException("Inconsistent fragmented snapshot metadata");
      }
      int offset = packet.index() * CHUNK_BYTES;
      if (this.received.get(packet.index())) {
        if (!Arrays.equals(
            this.bytes,
            offset,
            offset + packet.data().length,
            packet.data(),
            0,
            packet.data().length)) {
          throw new IllegalArgumentException("Conflicting duplicate fragment");
        }
      } else {
        System.arraycopy(packet.data(), 0, this.bytes, offset, packet.data().length);
        this.received.set(packet.index());
      }
      this.lastReceived = System.nanoTime();
      return this.received.cardinality() == this.parts;
    }
  }
}
