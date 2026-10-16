package moze_intel.projecte.impl.capability;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.codec.PECodecHelper;
import moze_intel.projecte.network.PEStreamCodecs;
import moze_intel.projecte.network.packets.to_client.alch_bag.SyncAllBagDataPKT;
import moze_intel.projecte.network.packets.to_client.alch_bag.SyncBagsDataPKT;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class AlchBagImpl implements IAlchBagProvider {
  private final Player player;

  public AlchBagImpl(Player player) {
    this.player = player;
  }

  private AlchBagImpl.AlchemicalBagAttachment attachment() {
    return (AlchBagImpl.AlchemicalBagAttachment)
        this.player.getAttachedOrCreate(PEAttachmentTypes.ALCHEMICAL_BAGS);
  }

  @NotNull
  @Override
  public IItemHandler getBag(@NotNull DyeColor color) {
    return this.attachment().getBag(color);
  }

  @Override
  public void sync(@NotNull ServerPlayer player, @NotNull Set<DyeColor> colors) {
    if (!colors.isEmpty()) {
      AlchBagImpl.AlchemicalBagAttachment attachment = this.attachment();
      Map<DyeColor, ItemStackHandler> handlers = new EnumMap<>(DyeColor.class);

      for (DyeColor color : colors) {
        handlers.put(color, attachment.getBag(color));
      }

      PacketDistributor.sendToPlayer(player, new SyncBagsDataPKT(handlers));
    }
  }

  @Override
  public void syncAllBags(@NotNull ServerPlayer player) {
    PacketDistributor.sendToPlayer(player, new SyncAllBagDataPKT(this.attachment()));
  }

  public static class AlchemicalBagAttachment {
    private static final int BAG_SIZE = 104;
    public static final Codec<AlchBagImpl.AlchemicalBagAttachment> CODEC =
        Codec.unboundedMap(DyeColor.CODEC, PECodecHelper.MUTABLE_HANDLER_CODEC)
            .xmap(
                map ->
                    new AlchBagImpl.AlchemicalBagAttachment(
                        map.isEmpty() ? new EnumMap<>(DyeColor.class) : new EnumMap<>(map)),
                attachment -> attachment.inventories);
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<DyeColor, ItemStackHandler>>
        MAP_STREAM_CODEC =
            ByteBufCodecs.map(
                ignored -> new EnumMap(DyeColor.class),
                DyeColor.STREAM_CODEC,
                PEStreamCodecs.handlerStreamCodec(104));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchBagImpl.AlchemicalBagAttachment>
        STREAM_CODEC =
            MAP_STREAM_CODEC.map(
                AlchBagImpl.AlchemicalBagAttachment::new, attachment -> attachment.inventories);
    private final Map<DyeColor, ItemStackHandler> inventories;

    public AlchemicalBagAttachment() {
      this(new EnumMap<>(DyeColor.class));
    }

    private AlchemicalBagAttachment(Map<DyeColor, ItemStackHandler> inventories) {
      this.inventories = inventories;
    }

    public AlchBagImpl.AlchemicalBagAttachment copy() {
      AlchBagImpl.AlchemicalBagAttachment copy = new AlchBagImpl.AlchemicalBagAttachment();

      for (Entry<DyeColor, ItemStackHandler> entry : this.inventories.entrySet()) {
        copy.inventories.put(
            entry.getKey(), PEAttachmentTypes.copyHandler(entry.getValue(), ItemStackHandler::new));
      }

      return copy;
    }

    public boolean mergeMigratedData(AlchBagImpl.AlchemicalBagAttachment legacy) {
      boolean complete = true;

      for (Entry<DyeColor, ItemStackHandler> entry : legacy.inventories.entrySet()) {
        ItemStackHandler legacyInventory = entry.getValue();
        ItemStackHandler currentInventory = this.inventories.get(entry.getKey());
        if (currentInventory == null) {
          this.inventories.put(
              entry.getKey(),
              PEAttachmentTypes.copyHandler(legacyInventory, ItemStackHandler::new));
        } else {
          for (int slot = 0; slot < legacyInventory.getSlots(); slot++) {
            ItemStack legacyStack = legacyInventory.getStackInSlot(slot);
            if (!legacyStack.isEmpty()) {
              if (slot >= currentInventory.getSlots()) {
                complete = false;
              } else {
                ItemStack currentStack = currentInventory.getStackInSlot(slot);
                if (currentStack.isEmpty()) {
                  currentInventory.setStackInSlot(slot, legacyStack.copy());
                } else if (!ItemStack.matches(currentStack, legacyStack)) {
                  complete = false;
                }
              }
            }
          }
        }
      }

      return complete;
    }

    @NotNull
    public ItemStackHandler getBag(@NotNull DyeColor color) {
      return this.inventories.computeIfAbsent(color, c -> new ItemStackHandler(104));
    }

    public void updateBags(Map<DyeColor, ItemStackHandler> handlers) {
      for (Entry<DyeColor, ItemStackHandler> entry : handlers.entrySet()) {
        DyeColor color = entry.getKey();
        ItemStackHandler handler = entry.getValue();
        if (handler.getSlots() == 104) {
          this.inventories.put(color, handler);
        } else {
          PECore.LOGGER.warn(
              "Received packet for updating {}, but the handler was of the wrong size. Expected:"
                  + " {}, Received: {}",
              new Object[] {color, 104, handler.getSlots()});
        }
      }
    }
  }
}
