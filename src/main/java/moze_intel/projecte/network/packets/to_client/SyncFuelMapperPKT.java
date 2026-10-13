package moze_intel.projecte.network.packets.to_client;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public record SyncFuelMapperPKT(HolderSet<Item> items) implements IPEPacket {
  public static final Type<SyncFuelMapperPKT> TYPE = new Type(PECore.rl("sync_fuel_mapper"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncFuelMapperPKT> STREAM_CODEC =
      ByteBufCodecs.holderSet(Registries.ITEM)
          .map(SyncFuelMapperPKT::new, SyncFuelMapperPKT::items);

  @NotNull
  public Type<SyncFuelMapperPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    FuelMapper.setFuelMap(this.items);
  }
}
