package moze_intel.projecte.compat.event.entity.living;

import moze_intel.projecte.compat.eventbus.Event;
import net.minecraft.world.entity.LivingEntity;

public class LivingEvent extends Event {
  private final LivingEntity entity;

  public LivingEvent(LivingEntity entity) {
    this.entity = entity;
  }

  public LivingEntity getEntity() {
    return this.entity;
  }

  public static final class LivingJumpEvent extends LivingEvent {
    public LivingJumpEvent(LivingEntity entity) {
      super(entity);
    }
  }
}
