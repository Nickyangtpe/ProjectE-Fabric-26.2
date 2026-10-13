package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.compat.event.EventHooks;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.EntityRandomizerHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.rabbit.Rabbit.RabbitGroupData;
import net.minecraft.world.entity.animal.rabbit.Rabbit.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EntityMobRandomizer extends NoGravityThrowableProjectile {
  public EntityMobRandomizer(EntityType<EntityMobRandomizer> type, Level level) {
    super(type, level);
  }

  public EntityMobRandomizer(Player entity, Level level) {
    super(
        (EntityType<? extends ThrowableProjectile>) PEEntityTypes.MOB_RANDOMIZER.get(),
        entity,
        level);
  }

  public void handleEntityEvent(byte id) {
    if (id == 3) {
      for (int i = 0; i < 8; i++) {
        this.level()
            .addParticle(
                ParticleTypes.PORTAL,
                this.getX(),
                this.getY() + this.random.nextDouble() * 2.0,
                this.getZ(),
                this.random.nextGaussian(),
                0.0,
                this.random.nextGaussian());
      }
    }
  }

  protected void onHitEntity(@NotNull EntityHitResult result) {
    super.onHitEntity(result);
    if (!this.level().isClientSide()
        && result.getEntity() instanceof Mob ent
        && this.getOwner() instanceof Player player) {
      ServerLevel level = (ServerLevel) this.level();
      Mob randomized = EntityRandomizerHelper.getRandomEntity(level, ent);
      if (randomized != null && EMCHelper.consumePlayerFuel(player, 384L) != -1L) {
        randomized.setPos(ent.position());
        randomized.setYRot(ent.getYRot());
        randomized.setXRot(ent.getXRot());
        SpawnGroupData data;
        if (randomized instanceof Rabbit rabbit && rabbit.getVariant() == Variant.EVIL) {
          data = new RabbitGroupData(Variant.EVIL);
        } else {
          data = null;
        }

        EventHooks.MobSpawnResult spawnResult =
            EventHooks.finalizeMobSpawnWithEvent(
                randomized,
                level,
                level.getCurrentDifficultyAt(randomized.blockPosition()),
                EntitySpawnReason.CONVERSION,
                data);
        if (!spawnResult.spawnCancelled() && level.tryAddFreshEntityWithPassengers(randomized)) {
          randomized.spawnAnim();
          ent.discard();
        }
      }
    }
  }
}
