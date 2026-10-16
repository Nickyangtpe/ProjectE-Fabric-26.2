package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.level.BlockEvent;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EntityWaterProjectile extends NoGravityThrowableProjectile {
  public EntityWaterProjectile(EntityType<EntityWaterProjectile> type, Level level) {
    super(type, level);
  }

  public EntityWaterProjectile(Player entity, Level level) {
    super(
        (EntityType<? extends ThrowableProjectile>) PEEntityTypes.WATER_PROJECTILE.get(),
        entity,
        level);
  }

  protected void onInsideBlock(@NotNull BlockState state) {
    if (!this.level().isClientSide() && state.getFluidState().is(FluidTags.WATER)) {
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
          BlockState state = level.getBlockState(pos);
          FluidState fluidState = state.getFluidState();
          if (fluidState.is(FluidTags.LAVA)) {
            pos = pos.immutable();
            if (state.getBlock() instanceof LiquidBlock) {
              Block block = fluidState.isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
              BlockEvent.FluidPlaceBlockEvent event =
                  new BlockEvent.FluidPlaceBlockEvent(level, pos, pos, block.defaultBlockState());
              if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
                PlayerHelper.checkedPlaceBlock(player, level, pos, event.getNewState());
              }
            } else {
              WorldHelper.drainFluid(player, level, pos, state);
            }

            this.playSound(
                SoundEvents.GENERIC_BURN,
                0.5F,
                2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);
          }
        }
      }

      if (this.getY() > level.getMaxY()) {
        ((ServerLevel) level).getWeatherData().setRaining(true);
        this.discard();
      }
    }
  }

  protected void onHitBlock(@NotNull BlockHitResult result) {
    super.onHitBlock(result);
    if (!this.level().isClientSide() && this.getOwner() instanceof Player player) {
      WorldHelper.placeFluid(
          player,
          this.level(),
          result.getBlockPos(),
          result.getDirection(),
          Fluids.WATER,
          !ProjectEConfig.server.items.opEvertide.get());
    }
  }

  protected void onHitEntity(@NotNull EntityHitResult result) {
    super.onHitEntity(result);
    if (!this.level().isClientSide()) {
      Entity ent = result.getEntity();
      if (ent.isOnFire()) {
        ent.clearFire();
      }

      ent.push(this.getDeltaMovement().scale(2.0));
    }
  }
}
