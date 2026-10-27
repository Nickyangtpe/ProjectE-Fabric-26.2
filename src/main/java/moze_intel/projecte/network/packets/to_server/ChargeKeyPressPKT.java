package moze_intel.projecte.network.packets.to_server;

import io.netty.buffer.ByteBuf;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/** Carries the client-observed secondary-use state to avoid crouch/key packet ordering races. */
public record ChargeKeyPressPKT(boolean secondaryUse) implements IPEPacket {
  public static final Type<ChargeKeyPressPKT> TYPE = new Type<>(PECore.rl("charge_key_press"));
  public static final StreamCodec<ByteBuf, ChargeKeyPressPKT> STREAM_CODEC =
      ByteBufCodecs.BOOL.map(ChargeKeyPressPKT::new, ChargeKeyPressPKT::secondaryUse);

  @Override
  public Type<ChargeKeyPressPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    if (!context.player().isSpectator()) {
      KeyPressPKT.handleCharge(context.player(), this.secondaryUse);
    }
  }
}
