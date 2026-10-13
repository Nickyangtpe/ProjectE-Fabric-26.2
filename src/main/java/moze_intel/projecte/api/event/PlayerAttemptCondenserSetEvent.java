package moze_intel.projecte.api.event;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.ICancellableEvent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerAttemptCondenserSetEvent extends Event implements ICancellableEvent {
  private final Player player;
  private final ItemInfo sourceInfo;
  private final ItemInfo reducedInfo;

  public PlayerAttemptCondenserSetEvent(
      @NotNull Player entityPlayer, @NotNull ItemInfo sourceInfo, @NotNull ItemInfo reducedInfo) {
    this.player = entityPlayer;
    this.sourceInfo = sourceInfo;
    this.reducedInfo = reducedInfo;
  }

  @NotNull
  public Player getPlayer() {
    return this.player;
  }

  @NotNull
  public ItemInfo getSourceInfo() {
    return this.sourceInfo;
  }

  @NotNull
  public ItemInfo getReducedInfo() {
    return this.reducedInfo;
  }
}
