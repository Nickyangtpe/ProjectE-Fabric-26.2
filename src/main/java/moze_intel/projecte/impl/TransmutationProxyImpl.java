package moze_intel.projecte.impl;

import java.util.Objects;
import java.util.UUID;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class TransmutationProxyImpl implements ITransmutationProxy {
  @NotNull
  @Override
  public IKnowledgeProvider getKnowledgeProviderFor(@NotNull UUID playerUUID) {
    Objects.requireNonNull(playerUUID);
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    boolean serverThread = server != null && server.getRunningThread() == Thread.currentThread();
    if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER && !serverThread) {
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
        Player clientPlayer =
            Objects.requireNonNull(Minecraft.getInstance().player, "Client player doesn't exist!");
        return Objects.requireNonNull(PECapabilities.KNOWLEDGE_CAPABILITY.find(clientPlayer, null));
      } else {
        throw new IllegalStateException("unreachable");
      }
    } else {
      server = Objects.requireNonNull(server, "Server must be running to query knowledge!");
      Player player = server.getPlayerList().getPlayer(playerUUID);
      return player != null
          ? Objects.requireNonNull(PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null))
          : TransmutationOffline.forPlayer(server, playerUUID);
    }
  }
}
