package moze_intel.projecte.network.packets;

import moze_intel.projecte.compat.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface IPEPacket extends CustomPacketPayload {
  void handle(IPayloadContext var1);
}
