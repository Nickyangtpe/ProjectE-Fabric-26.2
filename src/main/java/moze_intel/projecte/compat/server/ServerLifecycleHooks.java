package moze_intel.projecte.compat.server;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServerLifecycleHooks {
  @Nullable private static volatile MinecraftServer currentServer;

  private ServerLifecycleHooks() {}

  @Nullable
  public static MinecraftServer getCurrentServer() {
    return currentServer;
  }

  public static void setCurrentServer(@NotNull MinecraftServer server) {
    currentServer = server;
  }

  public static void clearCurrentServer(@NotNull MinecraftServer server) {
    if (currentServer == server) {
      currentServer = null;
    }
  }
}
