package moze_intel.projecte.compat.event.entity;

import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.ICancellableEvent;
import net.minecraft.world.entity.Entity;

public class EntityTeleportEvent extends Event implements ICancellableEvent {
  private final Entity entity;
  private double targetX;
  private double targetY;
  private double targetZ;

  public EntityTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
    this.entity = entity;
    this.targetX = targetX;
    this.targetY = targetY;
    this.targetZ = targetZ;
  }

  public Entity getEntity() {
    return this.entity;
  }

  public double getTargetX() {
    return this.targetX;
  }

  public void setTargetX(double targetX) {
    this.targetX = targetX;
  }

  public double getTargetY() {
    return this.targetY;
  }

  public void setTargetY(double targetY) {
    this.targetY = targetY;
  }

  public double getTargetZ() {
    return this.targetZ;
  }

  public void setTargetZ(double targetZ) {
    this.targetZ = targetZ;
  }
}
