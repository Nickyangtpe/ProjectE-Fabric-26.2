package moze_intel.projecte.compat.event.level;

import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.ICancellableEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;

public abstract class ExplosionEvent extends Event {
  private final ServerLevel level;
  private final Explosion explosion;

  protected ExplosionEvent(ServerLevel level, Explosion explosion) {
    this.level = level;
    this.explosion = explosion;
  }

  public ServerLevel getLevel() {
    return this.level;
  }

  public Explosion getExplosion() {
    return this.explosion;
  }

  public static final class Start extends ExplosionEvent implements ICancellableEvent {
    public Start(ServerLevel level, Explosion explosion) {
      super(level, explosion);
    }
  }
}
