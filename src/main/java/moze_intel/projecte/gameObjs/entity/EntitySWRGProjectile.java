package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.LightningHelper;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class EntitySWRGProjectile extends NoGravityThrowableProjectile {
  private boolean fromArcana = false;

  public EntitySWRGProjectile(EntityType<EntitySWRGProjectile> type, Level level) {
    super(type, level);
  }

  public EntitySWRGProjectile(Player player, boolean fromArcana, Level level) {
    super(
        (EntityType<? extends ThrowableProjectile>) PEEntityTypes.SWRG_PROJECTILE.get(),
        player,
        level);
    this.fromArcana = fromArcana;
  }

  @Override
  public void tick() {
    super.tick();
    if (this.isAlive()) {
      double inverse = 1.0 / (this.isInWater() ? 0.8 : 0.99);
      this.setDeltaMovement(this.getDeltaMovement().scale(inverse));
      if (!this.level().isClientSide()
          && this.isAlive()
          && this.getY() > this.level().getMinY() + this.level().getHeight()
          && this.level().isRaining()) {
        if (this.level() instanceof ServerLevel serverLevel) {
          serverLevel.getWeatherData().setThundering(true);
        }

        this.discard();
      }
    }
  }

  protected void onHitBlock(@NotNull BlockHitResult result) {
    super.onHitBlock(result);
    if (!this.level().isClientSide() && this.getOwner() instanceof ServerPlayer player) {
      ItemStack found =
          PlayerHelper.findFirstItem(
              player,
              this.fromArcana
                  ? PEItems.ARCANA_RING.get().builtInRegistryHolder()
                  : PEItems.SWIFTWOLF_RENDING_GALE.get().builtInRegistryHolder());
      if (!found.isEmpty() && ItemPE.consumeFuel(player, found, 768L, true)) {
        BlockPos pos = result.getBlockPos();
        LightningBolt lightning = LightningHelper.create(this.level());
        if (lightning != null) {
          lightning.setPos(Vec3.atCenterOf(pos));
          lightning.setCause(player);
          this.level().addFreshEntity(lightning);
        }

        if (this.level().isThundering()) {
          for (int i = 0; i < 3; i++) {
            LightningBolt bonus = LightningHelper.create(this.level());
            if (bonus != null) {
              bonus.setPos(
                  pos.getX() + 0.5 + this.level().getRandom().nextGaussian(),
                  pos.getY() + 0.5 + this.level().getRandom().nextGaussian(),
                  pos.getZ() + 0.5 + this.level().getRandom().nextGaussian());
              bonus.setCause(player);
              this.level().addFreshEntity(bonus);
            }
          }
        }
      }
    }
  }

  protected void onHitEntity(@NotNull EntityHitResult result) {
    super.onHitEntity(result);
    if (!this.level().isClientSide()
        && result.getEntity() instanceof LivingEntity e
        && this.getOwner() instanceof Player player) {
      ItemStack found =
          PlayerHelper.findFirstItem(
              player,
              this.fromArcana
                  ? PEItems.ARCANA_RING.get().builtInRegistryHolder()
                  : PEItems.SWIFTWOLF_RENDING_GALE.get().builtInRegistryHolder());
      if (!found.isEmpty() && ItemPE.consumeFuel(player, found, 64L, true)) {
        e.hurt(this.level().damageSources().playerAttack(player), 1.0F);
        boolean oldOnGround = e.onGround();
        e.setOnGround(true);
        e.knockback(
            5.0,
            -this.getDeltaMovement().x() * 0.25,
            -this.getDeltaMovement().z() * 0.25,
            this.level().damageSources().playerAttack(player),
            0.0F);
        e.setOnGround(oldOnGround);
        e.setDeltaMovement(e.getDeltaMovement().multiply(1.0, 3.0, 1.0));
      }
    }
  }

  public void readAdditionalSaveData(@NotNull ValueInput input) {
    super.readAdditionalSaveData(input);
    this.fromArcana = input.getBooleanOr("fromArcana", false);
  }

  public void addAdditionalSaveData(@NotNull ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putBoolean("fromArcana", this.fromArcana);
  }
}
