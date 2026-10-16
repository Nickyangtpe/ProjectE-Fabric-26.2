package moze_intel.projecte.gameObjs.entity;

import java.util.function.Predicate;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EntityFireProjectile extends NoGravityThrowableProjectile {
  private boolean fromArcana = false;

  public EntityFireProjectile(EntityType<EntityFireProjectile> type, Level level) {
    super(type, level);
  }

  public EntityFireProjectile(Player entity, boolean fromArcana, Level level) {
    super(
        (EntityType<? extends ThrowableProjectile>) PEEntityTypes.FIRE_PROJECTILE.get(),
        entity,
        level);
    this.fromArcana = fromArcana;
  }

  protected void onHitBlock(@NotNull BlockHitResult result) {
    super.onHitBlock(result);
    if (!this.level().isClientSide() && this.getOwner() instanceof Player player) {
      BlockPos pos = result.getBlockPos();
      BlockState state = this.level().getBlockState(pos);
      if (state.is(Blocks.OBSIDIAN)) {
        this.level().setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
      } else if (state.is(BlockTags.SAND)) {
        this.placeAOE(player, pos, 2, Blocks.GLASS.defaultBlockState(), s -> s.is(BlockTags.SAND));
      } else {
        this.placeAOE(player, pos, 1, Blocks.FIRE.defaultBlockState(), BlockStateBase::isAir);
      }
    }
  }

  private void placeAOE(
      Player player,
      BlockPos pos,
      int radius,
      BlockState newState,
      Predicate<BlockState> targetCheck) {
    Level level = this.level();

    for (BlockPos currentPos : WorldHelper.positionsAround(pos, radius)) {
      if (targetCheck.test(level.getBlockState(currentPos))) {
        PlayerHelper.checkedPlaceBlock(player, level, pos.immutable(), newState);
      }
    }
  }

  protected void onHitEntity(@NotNull EntityHitResult result) {
    super.onHitEntity(result);
    if (!this.level().isClientSide() && this.getOwner() instanceof Player player) {
      ItemStack found =
          PlayerHelper.findFirstItem(
              player,
              this.fromArcana
                  ? PEItems.ARCANA_RING.get().builtInRegistryHolder()
                  : PEItems.IGNITION_RING.get().builtInRegistryHolder());
      if (!found.isEmpty() && ItemPE.consumeFuel(player, found, 32L, true)) {
        Entity ent = result.getEntity();
        if (ent.hurtOrSimulate(this.level().damageSources().inFire(), 5.0F)) {
          ent.igniteForSeconds(5.0F);
        }
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
