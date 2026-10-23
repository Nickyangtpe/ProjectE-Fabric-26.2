package moze_intel.projecte.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Redirects native experience awards only while a ProjectE operation collects its block drops. */
public final class ExperienceDropTarget {
  private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

  private ExperienceDropTarget() {}

  public static Scope at(Level level, Vec3 position) {
    Scope scope = new Scope(level, position, ACTIVE.get());
    ACTIVE.set(scope);
    return scope;
  }

  public static Vec3 position(ServerLevel level, Vec3 original) {
    Scope scope = ACTIVE.get();
    return scope != null && scope.level == level ? scope.position : original;
  }

  public static final class Scope implements AutoCloseable {
    private final Level level;
    private final Vec3 position;
    private final Scope previous;
    private boolean closed;

    private Scope(Level level, Vec3 position, Scope previous) {
      this.level = level;
      this.position = position;
      this.previous = previous;
    }

    @Override
    public void close() {
      if (!this.closed) {
        if (ACTIVE.get() != this) {
          throw new IllegalStateException("Experience drop targets must close in reverse order");
        }
        if (this.previous == null) {
          ACTIVE.remove();
        } else {
          ACTIVE.set(this.previous);
        }
        this.closed = true;
      }
    }
  }
}
