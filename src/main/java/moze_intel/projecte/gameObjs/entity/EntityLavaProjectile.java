package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EntityLavaProjectile extends NoGravityThrowableProjectile {
  public EntityLavaProjectile(EntityType<EntityLavaProjectile> type, Level level) {
    super(type, level);
  }

  public EntityLavaProjectile(Player entity, Level level) {
    super(
        (EntityType<? extends ThrowableProjectile>) PEEntityTypes.LAVA_PROJECTILE.get(),
        entity,
        level);
  }

  protected void onInsideBlock(@NotNull BlockState state) {
    if (!this.level().isClientSide() && state.getFluidState().is(FluidTags.LAVA)) {
      this.discard();
    }
  }

  @Override
  public void tick() {
    super.tick();
    Level level = this.level();
    if (!level.isClientSide() && this.isAlive()) {
      if (this.getOwner() instanceof Player player) {
        for (BlockPos pos : WorldHelper.positionsAround(this.blockPosition(), 3)) {
          if (level.isLoaded(pos)) {
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.WATER)) {
              pos = pos.immutable();
              if (PlayerHelper.hasEditPermission(player, level, pos)) {
                WorldHelper.drainFluid(player, level, pos, state);
                level.playSound(
                    null,
                    pos,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);
              }
            }
          }
        }
      }

      if (this.getY() > level.getMaxY()) {
        ((ServerLevel) level).getWeatherData().setRaining(false);
        this.discard();
      }
    }
  }

  protected void onHitBlock(@NotNull BlockHitResult result) {
    super.onHitBlock(result);
    if (!this.level().isClientSide() && this.getOwner() instanceof Player player) {
      ItemStack found =
          PlayerHelper.findFirstItem(
              player, PEItems.VOLCANITE_AMULET.get().builtInRegistryHolder());
      if (!found.isEmpty() && ItemPE.consumeFuel(player, found, 32L, true)) {
        WorldHelper.placeFluid(
            player, this.level(), result.getBlockPos(), result.getDirection(), Fluids.LAVA, false);
      }
    }
  }

  protected void onHitEntity(@NotNull EntityHitResult result) {
    super.onHitEntity(result);
    if (!this.level().isClientSide() && this.getOwner() instanceof Player player) {
      ItemStack found =
          PlayerHelper.findFirstItem(
              player, PEItems.VOLCANITE_AMULET.get().builtInRegistryHolder());
      if (!found.isEmpty() && ItemPE.consumeFuel(player, found, 32L, true)) {
        Entity ent = result.getEntity();
        if (ent.hurtServer(
            (ServerLevel) this.level(), this.level().damageSources().inFire(), 5.0F)) {
          ent.igniteForSeconds(5.0F);
        }
      }
    }
  }
}
