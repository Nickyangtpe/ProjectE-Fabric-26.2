package moze_intel.projecte.network.packets.to_client.container;

import io.netty.buffer.ByteBuf;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.gameObjs.container.PEContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.NotNull;

public record UpdateWindowLongPKT(short windowId, short propId, long propVal) implements IPEPacket {
  public static final Type<UpdateWindowLongPKT> TYPE = new Type(PECore.rl("update_window_long"));
  public static final StreamCodec<ByteBuf, UpdateWindowLongPKT> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.SHORT,
          UpdateWindowLongPKT::windowId,
          ByteBufCodecs.SHORT,
          UpdateWindowLongPKT::propId,
          ByteBufCodecs.VAR_LONG,
          UpdateWindowLongPKT::propVal,
          UpdateWindowLongPKT::new);

  @NotNull
  public Type<UpdateWindowLongPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    if (context.player().containerMenu instanceof PEContainer container
        && container.containerId == this.windowId
        && this.propId >= 0
        && this.propId < container.longFieldCount()) {
      container.updateProgressBarLong(this.propId, this.propVal);
    }
  }
}
