package moze_intel.projecte.compat.fml.event.config;

import java.util.Objects;
import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.fml.config.ModConfig;

public abstract class ModConfigEvent extends Event {
  private final ModConfig config;

  protected ModConfigEvent(ModConfig config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  public ModConfig getConfig() {
    return this.config;
  }

  public static final class Loading extends ModConfigEvent {
    public Loading(ModConfig config) {
      super(config);
    }
  }

  public static final class Reloading extends ModConfigEvent {
    public Reloading(ModConfig config) {
      super(config);
    }
  }

  public static final class Unloading extends ModConfigEvent {
    public Unloading(ModConfig config) {
      super(config);
    }
  }
}
