package moze_intel.projecte.gameObjs.entity;

import java.util.Comparator;
import java.util.List;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class EntityHomingArrow extends Arrow {
  private static final EntityDataAccessor<Integer> DW_TARGET_ID =
      SynchedEntityData.defineId(EntityHomingArrow.class, EntityDataSerializers.INT);
  private static final double MAX_MAGNITUDE = Math.PI / 2;
  private static final int NO_TARGET = -1;
  private int newTargetCooldown = 0;

  public EntityHomingArrow(EntityType<EntityHomingArrow> type, Level level) {
    super(type, level);
  }

  public EntityHomingArrow(Level level, LivingEntity shooter, float damage) {
    super((EntityType) PEEntityTypes.HOMING_ARROW.get(), level);
    this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    this.setOwner(shooter);
    this.setBaseDamage(damage);
  }

  protected void defineSynchedData(@NotNull Builder builder) {
    super.defineSynchedData(builder);
    builder.define(DW_TARGET_ID, -1);
  }

  protected void doPostHurtEffects(@NotNull LivingEntity living) {
    super.doPostHurtEffects(living);
    living.invulnerableTime = 0;
  }

  public void tick() {
    if (this.tickCount > 3) {
      if (!this.level().isClientSide()) {
        Entity target = this.getTarget();
        if (target != null && (!target.isAlive() || this.isInGround())) {
          this.entityData.set(DW_TARGET_ID, -1);
          target = null;
        }

        if (target == null && !this.isInGround() && this.newTargetCooldown <= 0) {
          this.findNewTarget();
        } else {
          this.newTargetCooldown--;
        }
      }

      Entity target = this.getTarget();
      if (target != null && !this.isInGround()) {
        Vec3 arrowMotion = this.getDeltaMovement();
        Vec3 particlePos = this.position().add(arrowMotion.scale(0.25));
        Vec3 particleSpeed = arrowMotion.scale(-0.5).add(0.0, 0.2, 0.0);
        this.level()
            .addParticle(
                ParticleTypes.FLAME,
                particlePos.x(),
                particlePos.y(),
                particlePos.z(),
                particleSpeed.x(),
                particleSpeed.y(),
                particleSpeed.z());
        this.level()
            .addParticle(
                ParticleTypes.FLAME,
                particlePos.x(),
                particlePos.y(),
                particlePos.z(),
                particleSpeed.x(),
                particleSpeed.y(),
                particleSpeed.z());
        Vec3 targetLoc = target.position().add(0.0, target.getBbHeight() / 2.0F, 0.0);
        Vec3 lookVec = targetLoc.subtract(this.position());
        Vector3d adjustedLookVec = this.transform(arrowMotion, lookVec);
        this.shoot(adjustedLookVec.x, adjustedLookVec.y, adjustedLookVec.z, 1.0F, 0.0F);
      }
    }

    super.tick();
  }

  private Vector3d transform(Vec3 arrowMotion, Vec3 lookVec) {
    Vector3d normal = new Vector3d(arrowMotion.x, arrowMotion.y, arrowMotion.z);
    Vec3 axis = arrowMotion.cross(lookVec).normalize();
    if (axis == Vec3.ZERO) {
      return normal;
    }

    Vector3d look = new Vector3d(lookVec.x, lookVec.y, lookVec.z);
    double angle = Mth.clamp(normal.angle(look), -Math.PI / 2, Math.PI / 2);
    return new Matrix3d().rotation(angle, axis.x, axis.y, axis.z).transform(normal);
  }

  private void findNewTarget() {
    List<Mob> candidates =
        this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(8.0));
    if (!candidates.isEmpty()) {
      candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
      this.entityData.set(DW_TARGET_ID, candidates.getFirst().getId());
    }

    this.newTargetCooldown = 5;
  }

  @Nullable
  private Entity getTarget() {
    return this.level().getEntity((Integer) this.entityData.get(DW_TARGET_ID));
  }

  public boolean ignoreExplosion(@NotNull Explosion explosion) {
    return true;
  }
}
