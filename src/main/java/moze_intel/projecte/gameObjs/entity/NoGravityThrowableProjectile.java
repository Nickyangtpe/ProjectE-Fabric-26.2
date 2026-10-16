package moze_intel.projecte.gameObjs.entity;

import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public abstract class NoGravityThrowableProjectile extends ThrowableProjectile {
  protected NoGravityThrowableProjectile(
      EntityType<? extends ThrowableProjectile> type, Level level) {
    super(type, level);
    this.setNoGravity(true);
  }

  protected NoGravityThrowableProjectile(
      EntityType<? extends ThrowableProjectile> type, LivingEntity shooter, Level level) {
    super(type, shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ(), level);
    this.setOwner(shooter);
    this.setNoGravity(true);
  }

  protected void defineSynchedData(@NotNull Builder builder) {}

  public boolean ignoreExplosion(@NotNull Explosion explosion) {
    return true;
  }

  public void tick() {
    super.tick();
    if (!this.level().isClientSide()
        && (this.tickCount > 400 || !this.level().isLoaded(this.blockPosition()))) {
      this.discard();
    }
  }

  protected void onHit(@NotNull HitResult result) {
    super.onHit(result);
    if (!this.level().isClientSide()) {
      this.level().broadcastEntityEvent(this, (byte) 3);
      this.discard();
    }
  }
}
