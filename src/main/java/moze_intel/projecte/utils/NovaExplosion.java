package moze_intel.projecte.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public class NovaExplosion implements Explosion {
  private static final ExplosionDamageCalculator DEFAULT_DAMAGE_CALCULATOR =
      new ExplosionDamageCalculator();
  private final BlockInteraction blockInteraction;
  private final ServerLevel level;
  private final Vec3 center;
  @Nullable private final Entity source;
  private final float radius;
  private final DamageSource damageSource;
  private final ExplosionDamageCalculator damageCalculator;
  private final Map<Player, Vec3> hitPlayers = new HashMap<>();
  private List<BlockPos> explodedPositions = List.of();

  public NovaExplosion(
      ServerLevel level,
      @Nullable Entity source,
      double x,
      double y,
      double z,
      float radius,
      BlockInteraction blockInteraction) {
    this.level = level;
    this.source = source;
    this.center = new Vec3(x, y, z);
    this.radius = radius;
    this.blockInteraction = blockInteraction;
    this.damageSource = level.damageSources().explosion(this);
    this.damageCalculator =
        (ExplosionDamageCalculator)
            (source == null
                ? DEFAULT_DAMAGE_CALCULATOR
                : new EntityBasedExplosionDamageCalculator(source));
  }

  public static float getSeenPercent(Vec3 center, Entity entity) {
    AABB box = entity.getBoundingBox();
    double xs = 1.0 / (box.getXsize() * 2.0 + 1.0);
    double ys = 1.0 / (box.getYsize() * 2.0 + 1.0);
    double zs = 1.0 / (box.getZsize() * 2.0 + 1.0);
    double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
    double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
    int hits = 0;
    int count = 0;

    for (double xx = 0.0; xx <= 1.0; xx += xs) {
      for (double yy = 0.0; yy <= 1.0; yy += ys) {
        for (double zz = 0.0; zz <= 1.0; zz += zs) {
          Vec3 from =
              new Vec3(
                  Mth.lerp(xx, box.minX, box.maxX) + xOffset,
                  Mth.lerp(yy, box.minY, box.maxY),
                  Mth.lerp(zz, box.minZ, box.maxZ) + zOffset);
          if (entity
                  .level()
                  .clip(new ClipContext(from, center, Block.COLLIDER, Fluid.NONE, entity))
                  .getType()
              == Type.MISS) {
            hits++;
          }

          count++;
        }
      }
    }

    return (float) hits / count;
  }

  private List<BlockPos> calculateExplodedPositions() {
    Set<BlockPos> toBlow = new HashSet<>();

    for (int xx = 0; xx < 16; xx++) {
      for (int yy = 0; yy < 16; yy++) {
        for (int zz = 0; zz < 16; zz++) {
          if (xx == 0 || xx == 15 || yy == 0 || yy == 15 || zz == 0 || zz == 15) {
            double xd = xx / 15.0F * 2.0F - 1.0F;
            double yd = yy / 15.0F * 2.0F - 1.0F;
            double zd = zz / 15.0F * 2.0F - 1.0F;
            double length = Math.sqrt(xd * xd + yd * yd + zd * zd);
            xd /= length;
            yd /= length;
            zd /= length;
            double x = this.center.x;
            double y = this.center.y;
            double z = this.center.z;

            for (float power = this.radius * (0.7F + this.level.getRandom().nextFloat() * 0.6F);
                power > 0.0F;
                power -= 0.22500001F) {
              BlockPos pos = BlockPos.containing(x, y, z);
              BlockState state = this.level.getBlockState(pos);
              FluidState fluid = this.level.getFluidState(pos);
              if (!this.level.isInWorldBounds(pos)) {
                break;
              }

              Optional<Float> resistance =
                  this.damageCalculator.getBlockExplosionResistance(
                      this, this.level, pos, state, fluid);
              if (resistance.isPresent()) {
                power -= (resistance.get() + 0.3F) * 0.3F;
              }

              if (power > 0.0F
                  && this.damageCalculator.shouldBlockExplode(
                      this, this.level, pos, state, power)) {
                toBlow.add(pos);
              }

              x += xd * 0.3F;
              y += yd * 0.3F;
              z += zd * 0.3F;
            }
          }
        }
      }
    }

    return new ObjectArrayList(toBlow);
  }

  private void hurtEntities() {
    if (!(this.radius < 1.0E-5F)) {
      float doubleRadius = this.radius * 2.0F;
      int x0 = Mth.floor(this.center.x - doubleRadius - 1.0);
      int x1 = Mth.floor(this.center.x + doubleRadius + 1.0);
      int y0 = Mth.floor(this.center.y - doubleRadius - 1.0);
      int y1 = Mth.floor(this.center.y + doubleRadius + 1.0);
      int z0 = Mth.floor(this.center.z - doubleRadius - 1.0);
      int z1 = Mth.floor(this.center.z + doubleRadius + 1.0);

      for (Entity entity : this.level.getEntities(this.source, new AABB(x0, y0, z0, x1, y1, z1))) {
        if (!entity.ignoreExplosion(this)) {
          double distance = Math.sqrt(entity.distanceToSqr(this.center)) / doubleRadius;
          if (!(distance > 1.0)) {
            Vec3 origin = entity instanceof PrimedTnt ? entity.position() : entity.getEyePosition();
            Vec3 direction = origin.subtract(this.center).normalize();
            boolean damage = this.damageCalculator.shouldDamageEntity(this, entity);
            float knockbackMultiplier = this.damageCalculator.getKnockbackMultiplier(entity);
            float exposure =
                !damage && knockbackMultiplier == 0.0F ? 0.0F : getSeenPercent(this.center, entity);
            if (damage) {
              entity.hurtServer(
                  this.level,
                  this.damageSource,
                  this.damageCalculator.getEntityDamageAmount(this, entity, exposure));
            }

            double resistance =
                entity instanceof LivingEntity living
                    ? living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                    : 0.0;
            Vec3 knockback =
                direction.scale(
                    (1.0 - distance) * exposure * knockbackMultiplier * (1.0 - resistance));
            entity.push(knockback);
            if (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
                && entity instanceof Projectile projectile) {
              projectile.setOwner(this.damageSource.getEntity());
            } else if (entity instanceof Player player
                && !player.isSpectator()
                && (!player.isCreative() || !player.getAbilities().flying)) {
              this.hitPlayers.put(player, knockback);
            }

            entity.onExplosionHit(this.source);
          }
        }
      }
    }
  }

  private void interactWithBlocks(List<BlockPos> targetBlocks) {
    NonNullList<ItemStack> allDrops = NonNullList.create();
    Util.shuffle(targetBlocks, this.level.getRandom());

    for (BlockPos pos : targetBlocks) {
      this.level
          .getBlockState(pos)
          .onExplosionHit(this.level, pos, this, (stack, position) -> allDrops.add(stack));
    }

    LivingEntity placer = this.getIndirectSourceEntity();
    WorldHelper.createLootDrop(
        allDrops, this.level, placer == null ? this.center : placer.position());
  }

  public List<BlockPos> explode() {
    this.level.gameEvent(this.source, GameEvent.EXPLODE, this.center);
    this.explodedPositions = this.calculateExplodedPositions();
    this.hurtEntities();
    if (this.blockInteraction != BlockInteraction.KEEP) {
      this.interactWithBlocks(this.explodedPositions);
    }

    return this.explodedPositions;
  }

  public List<BlockPos> finalizeExplosion() {
    return this.explodedPositions;
  }

  public ServerLevel level() {
    return this.level;
  }

  @Nullable
  public LivingEntity getIndirectSourceEntity() {
    return Explosion.getIndirectSourceEntity(this.source);
  }

  @Nullable
  public Entity getDirectSourceEntity() {
    return this.source;
  }

  public BlockInteraction getBlockInteraction() {
    return this.blockInteraction;
  }

  public float radius() {
    return this.radius;
  }

  public Vec3 center() {
    return this.center;
  }

  public boolean canTriggerBlocks() {
    return false;
  }

  public boolean shouldAffectBlocklikeEntities() {
    return (Boolean) this.level.getGameRules().get(GameRules.MOB_GRIEFING);
  }

  public Map<Player, Vec3> getHitPlayers() {
    return this.hitPlayers;
  }
}
