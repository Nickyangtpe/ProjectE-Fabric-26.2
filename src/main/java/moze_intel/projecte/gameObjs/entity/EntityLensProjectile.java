package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.gameObjs.items.HyperkineticLens;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class EntityLensProjectile extends NoGravityThrowableProjectile {
  private HyperkineticLens.ExplosiveLensCharge charge;

  public EntityLensProjectile(EntityType<EntityLensProjectile> type, Level level) {
    super(type, level);
    this.charge = HyperkineticLens.ExplosiveLensCharge.UNCHARGED;
  }

  public EntityLensProjectile(Player entity, HyperkineticLens.ExplosiveLensCharge charge) {
    super(
        (EntityType<? extends ThrowableProjectile>) PEEntityTypes.LENS_PROJECTILE.get(),
        entity,
        entity.level());
    this.charge = charge;
  }

  protected void onInsideBlock(@NotNull BlockState state) {
    if (!this.level().isClientSide() && state.getFluidState().is(FluidTags.WATER)) {
      this.playSound(
          SoundEvents.GENERIC_BURN,
          0.7F,
          1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
      ((ServerLevel) this.level())
          .sendParticles(
              ParticleTypes.LARGE_SMOKE,
              this.getX(),
              this.getY(),
              this.getZ(),
              2,
              0.0,
              0.0,
              0.0,
              0.0);
      this.discard();
    }
  }

  @Override
  protected void onHit(@NotNull HitResult result) {
    super.onHit(result);
    if (!this.level().isClientSide()) {
      WorldHelper.createNovaExplosion(
          this.level(),
          this.getOwner(),
          this.getX(),
          this.getY(),
          this.getZ(),
          this.charge.radius());
    }
  }

  public void addAdditionalSaveData(@NotNull ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putInt("charge", this.charge.ordinal());
  }

  public void readAdditionalSaveData(@NotNull ValueInput input) {
    super.readAdditionalSaveData(input);
    this.charge = HyperkineticLens.ExplosiveLensCharge.BY_ID.apply(input.getIntOr("charge", 0));
  }
}
