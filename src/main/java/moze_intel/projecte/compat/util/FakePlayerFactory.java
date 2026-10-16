package moze_intel.projecte.compat.util;

import com.mojang.authlib.GameProfile;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

public final class FakePlayerFactory {
  private static final Map<ServerLevel, Map<UUID, WeakReference<FakePlayer>>> PLAYERS =
      new WeakHashMap<>();

  private FakePlayerFactory() {}

  public static FakePlayer get(ServerLevel level, GameProfile profile) {
    synchronized (PLAYERS) {
      Map<UUID, WeakReference<FakePlayer>> perWorld =
          PLAYERS.computeIfAbsent(level, ignored -> new HashMap<>());
      WeakReference<FakePlayer> reference = perWorld.get(profile.id());
      FakePlayer player = reference == null ? null : reference.get();
      if (player == null) {
        player = new FakePlayer(level, profile);
        perWorld.put(profile.id(), new WeakReference<>(player));
      }

      return player;
    }
  }
}
