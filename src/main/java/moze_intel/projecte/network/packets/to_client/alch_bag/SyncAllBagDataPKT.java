package moze_intel.projecte.network.packets.to_client.alch_bag;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.NotNull;

public record SyncAllBagDataPKT(AlchBagImpl.AlchemicalBagAttachment data) implements IPEPacket {
  public static final Type<SyncAllBagDataPKT> TYPE = new Type(PECore.rl("sync_all_bag_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncAllBagDataPKT> STREAM_CODEC =
      AlchBagImpl.AlchemicalBagAttachment.STREAM_CODEC.map(
          SyncAllBagDataPKT::new, SyncAllBagDataPKT::data);

  @NotNull
  public Type<SyncAllBagDataPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      player.setAttached(PEAttachmentTypes.ALCHEMICAL_BAGS, this.data);
    }

    PECore.debugLog("** RECEIVED BAGS CLIENTSIDE **");
  }
}
