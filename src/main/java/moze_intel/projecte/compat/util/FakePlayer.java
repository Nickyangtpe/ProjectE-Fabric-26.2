package moze_intel.projecte.compat.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FakePlayer extends ServerPlayer {
  public FakePlayer(ServerLevel level, GameProfile profile) {
    super(level.getServer(), level, profile, ClientInformation.createDefault());
  }
}
