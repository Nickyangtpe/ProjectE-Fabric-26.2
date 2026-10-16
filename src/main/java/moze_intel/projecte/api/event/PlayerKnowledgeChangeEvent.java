package moze_intel.projecte.api.event;

import java.util.UUID;
import moze_intel.projecte.compat.eventbus.Event;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerKnowledgeChangeEvent extends Event {
  private final UUID playerUUID;

  public PlayerKnowledgeChangeEvent(@NotNull Player player) {
    this(player.getUUID());
  }

  public PlayerKnowledgeChangeEvent(@NotNull UUID playerUUID) {
    this.playerUUID = playerUUID;
  }

  @NotNull
  public UUID getPlayerUUID() {
    return this.playerUUID;
  }
}
