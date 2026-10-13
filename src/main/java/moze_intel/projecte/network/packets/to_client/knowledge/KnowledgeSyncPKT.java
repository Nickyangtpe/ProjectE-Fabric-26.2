package moze_intel.projecte.network.packets.to_client.knowledge;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.NotNull;

public record KnowledgeSyncPKT(KnowledgeImpl.KnowledgeAttachment data) implements IPEPacket {
  public static final Type<KnowledgeSyncPKT> TYPE = new Type(PECore.rl("sync_knowledge"));
  public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeSyncPKT> STREAM_CODEC =
      KnowledgeImpl.KnowledgeAttachment.STREAM_CODEC.map(
          KnowledgeSyncPKT::new, KnowledgeSyncPKT::data);

  @NotNull
  public Type<KnowledgeSyncPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      player.setAttached(PEAttachmentTypes.KNOWLEDGE, this.data);
      if (player.containerMenu instanceof TransmutationContainer container) {
        container.transmutationInventory.updateClientTargets(false);
      }
    }

    PECore.debugLog("** RECEIVED TRANSMUTATION DATA CLIENTSIDE **");
  }
}
