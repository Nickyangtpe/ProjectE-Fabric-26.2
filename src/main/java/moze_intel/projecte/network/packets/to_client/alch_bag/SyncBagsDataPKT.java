package moze_intel.projecte.network.packets.to_client.alch_bag;

import java.util.Map;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

public record SyncBagsDataPKT(Map<DyeColor, ItemStackHandler> handlers) implements IPEPacket {
  public static final Type<SyncBagsDataPKT> TYPE = new Type(PECore.rl("sync_bag_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncBagsDataPKT> STREAM_CODEC =
      AlchBagImpl.AlchemicalBagAttachment.MAP_STREAM_CODEC.map(
          SyncBagsDataPKT::new, SyncBagsDataPKT::handlers);

  @NotNull
  public Type<SyncBagsDataPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      ((AlchBagImpl.AlchemicalBagAttachment)
              player.getAttachedOrCreate(PEAttachmentTypes.ALCHEMICAL_BAGS))
          .updateBags(this.handlers);
    }

    PECore.debugLog("** RECEIVED BAGS CLIENTSIDE **");
  }
}
