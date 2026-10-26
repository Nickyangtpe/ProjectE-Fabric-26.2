package moze_intel.projecte.network;

import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.compat.eventbus.IEventBus;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.compat.network.event.RegisterPayloadHandlersEvent;
import moze_intel.projecte.compat.network.handling.IPayloadHandler;
import moze_intel.projecte.compat.network.registration.PayloadRegistrar;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.items.rings.ArchangelSmite;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.FragmentedSyncPKT;
import moze_intel.projecte.network.packets.to_client.NovaExplosionSyncPKT;
import moze_intel.projecte.network.packets.to_client.SyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.SyncFuelMapperPKT;
import moze_intel.projecte.network.packets.to_client.SyncWorldTransmutations;
import moze_intel.projecte.network.packets.to_client.alch_bag.SyncAllBagDataPKT;
import moze_intel.projecte.network.packets.to_client.alch_bag.SyncBagsDataPKT;
import moze_intel.projecte.network.packets.to_client.container.SyncOffhandPkt;
import moze_intel.projecte.network.packets.to_client.container.UpdateCondenserLockPKT;
import moze_intel.projecte.network.packets.to_client.container.UpdateWindowLongPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncInputsAndLocksPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncPKT;
import moze_intel.projecte.network.packets.to_server.KeyPressPKT;
import moze_intel.projecte.network.packets.to_server.RequestEmcSyncPKT;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import moze_intel.projecte.network.packets.to_server.UpdateGemModePKT;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PacketHandler {
  private PacketHandler.SimplePacketPayLoad activateArchangel;
  private PacketHandler.SimplePacketPayLoad clearKnowledge;
  private PacketHandler.SimplePacketPayLoad updateTransmutationTargets;
  private PacketHandler.SimplePacketPayLoad resetCooldown;

  public PacketHandler(IEventBus modEventBus, String version) {
    modEventBus.addListener(
        RegisterPayloadHandlersEvent.class,
        event -> {
          PayloadRegistrar registrar = event.registrar(version);
          this.registerClientToServer(new PacketHandler.PacketRegistrar(registrar, true));
          this.registerServerToClient(new PacketHandler.PacketRegistrar(registrar, false));
        });
  }

  private void registerClientToServer(PacketHandler.PacketRegistrar registrar) {
    registrar.play(KeyPressPKT.TYPE, KeyPressPKT.STREAM_CODEC);
    registrar.play(RequestEmcSyncPKT.TYPE, RequestEmcSyncPKT.STREAM_CODEC);
    this.activateArchangel =
        registrar.playInstanced(
            PECore.rl("activate_archangel"),
            (ignored, context) -> {
              Player player = context.player();
              ItemStack main = player.getMainHandItem();
              if (!main.isEmpty() && main.is(PEItems.ARCHANGEL_SMITE.get())) {
                ArchangelSmite.fireVolley(main, player);
              }
            });
    registrar.play(SearchUpdatePKT.TYPE, SearchUpdatePKT.STREAM_CODEC);
    registrar.play(UpdateGemModePKT.TYPE, UpdateGemModePKT.STREAM_CODEC);
  }

  private void registerServerToClient(PacketHandler.PacketRegistrar registrar) {
    this.resetCooldown =
        registrar.playInstanced(
            PECore.rl("reset_cooldown"),
            (ignored, context) -> context.player().resetAttackStrengthTicker());
    this.clearKnowledge =
        registrar.playInstanced(
            PECore.rl("clear_knowledge"),
            (ignored, context) -> {
              Player player = context.player();
              IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
              if (knowledge != null) {
                knowledge.clearKnowledge();
                if (player.containerMenu instanceof TransmutationContainer container) {
                  container.transmutationInventory.updateClientTargets(false);
                }
              }
            });
    registrar.play(KnowledgeSyncPKT.TYPE, KnowledgeSyncPKT.STREAM_CODEC);
    registrar.play(KnowledgeSyncEmcPKT.TYPE, KnowledgeSyncEmcPKT.STREAM_CODEC);
    registrar.play(
        KnowledgeSyncInputsAndLocksPKT.TYPE, KnowledgeSyncInputsAndLocksPKT.STREAM_CODEC);
    registrar.play(KnowledgeSyncChangePKT.TYPE, KnowledgeSyncChangePKT.STREAM_CODEC);
    registrar.play(NovaExplosionSyncPKT.TYPE, NovaExplosionSyncPKT.STREAM_CODEC);
    registrar.play(SyncAllBagDataPKT.TYPE, SyncAllBagDataPKT.STREAM_CODEC);
    registrar.play(SyncBagsDataPKT.TYPE, SyncBagsDataPKT.STREAM_CODEC);
    registrar.play(SyncEmcPKT.TYPE, SyncEmcPKT.STREAM_CODEC);
    registrar.play(FragmentedSyncPKT.TYPE, FragmentedSyncPKT.STREAM_CODEC);
    registrar.play(SyncOffhandPkt.TYPE, SyncOffhandPkt.STREAM_CODEC);
    registrar.play(SyncFuelMapperPKT.TYPE, SyncFuelMapperPKT.STREAM_CODEC);
    registrar.play(SyncWorldTransmutations.TYPE, SyncWorldTransmutations.STREAM_CODEC);
    registrar.play(UpdateCondenserLockPKT.TYPE, UpdateCondenserLockPKT.STREAM_CODEC);
    this.updateTransmutationTargets =
        registrar.playInstanced(
            PECore.rl("update_transmutation_targets"),
            (ignored, context) -> {
              if (context.player().containerMenu instanceof TransmutationContainer container) {
                container.transmutationInventory.updateClientTargets(false);
              }
            });
    registrar.play(UpdateWindowLongPKT.TYPE, UpdateWindowLongPKT.STREAM_CODEC);
  }

  public void clearKnowledge(ServerPlayer player) {
    PacketDistributor.sendToPlayer(player, this.clearKnowledge);
  }

  public void updateTransmutationTargets(ServerPlayer player) {
    PacketDistributor.sendToPlayer(player, this.updateTransmutationTargets);
  }

  public void resetCooldown(ServerPlayer player) {
    PacketDistributor.sendToPlayer(player, this.resetCooldown);
  }

  public void activateArchangel() {
    PacketDistributor.sendToServer(this.activateArchangel);
  }

  protected record PacketRegistrar(PayloadRegistrar registrar, boolean toServer) {
    public <MSG extends IPEPacket> void play(
        Type<MSG> type, StreamCodec<? super RegistryFriendlyByteBuf, MSG> reader) {
      if (this.toServer) {
        this.registrar.playToServer(type, reader, IPEPacket::handle);
      } else {
        this.registrar.playToClient(type, reader, IPEPacket::handle);
      }
    }

    public PacketHandler.SimplePacketPayLoad playInstanced(
        Identifier id, IPayloadHandler<CustomPacketPayload> handler) {
      PacketHandler.SimplePacketPayLoad payload = new PacketHandler.SimplePacketPayLoad(id);
      if (this.toServer) {
        this.registrar.playToServer(payload.type(), StreamCodec.unit(payload), handler);
      } else {
        this.registrar.playToClient(payload.type(), StreamCodec.unit(payload), handler);
      }

      return payload;
    }
  }

  protected record SimplePacketPayLoad(Type<CustomPacketPayload> type)
      implements CustomPacketPayload {
    private SimplePacketPayLoad(Identifier id) {
      this(new Type(id));
    }
  }
}
