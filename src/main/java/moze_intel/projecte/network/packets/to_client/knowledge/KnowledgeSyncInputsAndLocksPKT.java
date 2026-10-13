package moze_intel.projecte.network.packets.to_client.knowledge;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record KnowledgeSyncInputsAndLocksPKT(
    Int2ObjectMap<ItemStack> stacksToSync, IKnowledgeProvider.TargetUpdateType updateTargets)
    implements IPEPacket {
  public static final Type<KnowledgeSyncInputsAndLocksPKT> TYPE =
      new Type(PECore.rl("knowledge_sync_inputs_and_locks"));
  public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeSyncInputsAndLocksPKT>
      STREAM_CODEC =
          StreamCodec.composite(
              ByteBufCodecs.map(
                  Int2ObjectOpenHashMap::new,
                  ByteBufCodecs.VAR_INT,
                  ItemStack.OPTIONAL_STREAM_CODEC),
              KnowledgeSyncInputsAndLocksPKT::stacksToSync,
              IKnowledgeProvider.TargetUpdateType.STREAM_CODEC,
              KnowledgeSyncInputsAndLocksPKT::updateTargets,
              KnowledgeSyncInputsAndLocksPKT::new);

  @NotNull
  public Type<KnowledgeSyncInputsAndLocksPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    Player player = context.player();
    IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    if (knowledge != null) {
      knowledge.receiveInputsAndLocks(this.stacksToSync);
      if (this.updateTargets != IKnowledgeProvider.TargetUpdateType.NONE
          && player.containerMenu instanceof TransmutationContainer container) {
        TransmutationInventory transmutationInventory = container.transmutationInventory;
        if (this.updateTargets == IKnowledgeProvider.TargetUpdateType.ALL) {
          transmutationInventory.updateClientTargets(false);
        } else {
          transmutationInventory.checkForUpdates();
        }
      }
    }

    PECore.debugLog("** RECEIVED TRANSMUTATION INPUT AND LOCK DATA CLIENTSIDE **");
  }
}
