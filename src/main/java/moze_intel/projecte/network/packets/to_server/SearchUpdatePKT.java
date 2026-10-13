package moze_intel.projecte.network.packets.to_server;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record SearchUpdatePKT(int slot, ItemStack itemStack) implements IPEPacket {
  public static final Type<SearchUpdatePKT> TYPE = new Type(PECore.rl("update_search"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SearchUpdatePKT> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_INT,
          SearchUpdatePKT::slot,
          ItemStack.OPTIONAL_STREAM_CODEC,
          SearchUpdatePKT::itemStack,
          SearchUpdatePKT::new);

  public SearchUpdatePKT {
    itemStack = itemStack.copy();
  }

  @NotNull
  public Type<SearchUpdatePKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    if (context.player().containerMenu instanceof TransmutationContainer container
        && this.slot >= 0
        && this.slot < container.transmutationInventory.getOutputSlotCount()) {
      container.transmutationInventory.writeIntoOutputSlot(this.slot, this.itemStack);
    }
  }
}
