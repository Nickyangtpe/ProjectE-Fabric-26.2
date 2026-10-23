package moze_intel.projecte.utils;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.capabilities.BlockCapability;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ToolActionHelper;
import moze_intel.projecte.compat.event.EventHooks;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.compat.util.PESidedSuccess;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.network.packets.to_client.NovaExplosionSyncPKT;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.Property.Value;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorldHelper {
  private static final Predicate<Entity> SWRG_REPEL_PREDICATE =
      entity -> validRepelEntity(entity, PETags.Entities.BLACKLIST_SWRG);
  private static final Map<Block, IntegerProperty> AGE_PROPERTIES =
      new Reference2ObjectOpenHashMap();

  public static void clearCachedAgeProperties() {
    AGE_PROPERTIES.clear();
  }

  /**
   * Collects item loot and runs native after-break effects for a successfully removed block.
   * Callers must capture the state and block entity before removing or replacing the block.
   * Native experience awards follow the active collected-loot destination when one is set.
   */
  public static void collectBlockDrops(
      BlockState state,
      ServerLevel level,
      BlockPos pos,
      @Nullable BlockEntity blockEntity,
      Player player,
      ItemStack tool,
      List<ItemStack> drops) {
    drops.addAll(Block.getDrops(state, level, pos, blockEntity, player, tool));
    // getDrops only evaluates item loot. Ores, spawners, sculk and modded blocks
    // produce experience through this callback, including Silk Touch and game rules.
    state.spawnAfterBreak(level, pos, tool, !player.preventsBlockDrops());
  }

  public static void collectBlockDrops(
      BlockState state,
      ServerLevel level,
      BlockPos pos,
      @Nullable BlockEntity blockEntity,
      Player player,
      ItemStack tool,
      List<ItemStack> drops,
      Vec3 dropPosition) {
    try (ExperienceDropTarget.Scope ignored = ExperienceDropTarget.at(level, dropPosition)) {
      collectBlockDrops(state, level, pos, blockEntity, player, tool, drops);
    }
  }

  public static void createLootDrop(List<ItemStack> drops, Level level, BlockPos pos) {
    createLootDrop(drops, level, pos.getX(), pos.getY(), pos.getZ());
  }

  public static void createLootDrop(List<ItemStack> drops, Level level, Vec3 pos) {
    createLootDrop(drops, level, pos.x(), pos.y(), pos.z());
  }

  public static void createLootDrop(
      List<ItemStack> drops, Level level, double x, double y, double z) {
    if (!drops.isEmpty()) {
      List<ItemEntity> knownItems = new ArrayList<>();

      for (ItemStack drop : drops) {
        if (!drop.isEmpty()) {
          int dropCount = drop.getCount();
          ItemEntity itemEntity = null;

          for (ItemEntity knownItem : knownItems) {
            if (ItemStack.isSameItemSameComponents(knownItem.getItem(), drop)) {
              itemEntity = knownItem;
              break;
            }
          }

          if (itemEntity != null) {
            int availableRoom = drop.getMaxStackSize() - itemEntity.getItem().getCount();
            if (dropCount <= availableRoom) {
              itemEntity.getItem().grow(dropCount);
              if (dropCount == availableRoom) {
                knownItems.remove(itemEntity);
                level.addFreshEntity(itemEntity);
              }
              continue;
            }

            itemEntity.getItem().grow(availableRoom);
            knownItems.remove(itemEntity);
            level.addFreshEntity(itemEntity);
            dropCount -= availableRoom;
          }

          itemEntity = new ItemEntity(level, x, y, z, drop.copyWithCount(dropCount));
          knownItems.add(itemEntity);
        }
      }

      for (ItemEntity itemEntity : knownItems) {
        level.addFreshEntity(itemEntity);
      }
    }
  }

  public static void createNovaExplosion(
      Level level, Entity exploder, double x, double y, double z, float power) {
    if (level instanceof ServerLevel serverLevel) {
      BlockInteraction mode =
          serverLevel.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)
              ? BlockInteraction.DESTROY_WITH_DECAY
              : BlockInteraction.DESTROY;
      NovaExplosion explosion = new NovaExplosion(serverLevel, exploder, x, y, z, power, mode);
      if (EventHooks.onExplosionStart(serverLevel, explosion)) {
        return;
      }

      explosion.explode();
      List<BlockPos> particlePositions = explosion.finalizeExplosion();
      NovaExplosionSyncPKT packet =
          new NovaExplosionSyncPKT(
              explosion.center(),
              explosion.radius(),
              SoundEvents.GENERIC_EXPLODE,
              particlePositions);

      for (ServerPlayer player : serverLevel.players()) {
        if (player.distanceToSqr(x, y, z) < 4096.0) {
          PacketDistributor.sendToPlayer(player, packet);
        }
      }
    }
  }

  public static void drainFluid(
      @Nullable Player player, Level level, BlockPos pos, BlockState state) {
    if (state.getBlock() instanceof BucketPickup bucketPickup) {
      bucketPickup.pickupBlock(player, level, pos, state);
    }
  }

  public static void dropInventory(@Nullable IItemHandler inv, Level level, BlockPos pos) {
    if (inv != null) {
      int i = 0;

      for (int slots = inv.getSlots(); i < slots; i++) {
        ItemStack stack = inv.getStackInSlot(i);
        if (!stack.isEmpty()) {
          level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack));
        }
      }
    }
  }

  public static void extinguishNearby(Level level, Player player) {
    for (BlockPos pos : getPositionsInBox(player.getBoundingBox().inflate(1.0))) {
      pos = pos.immutable();
      if (level.getBlockState(pos).is(Blocks.FIRE)
          && PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
        level.removeBlock(pos, false);
      }
    }
  }

  public static void freezeInBoundingBox(Level level, AABB box, Player player, boolean random) {
    for (BlockPos pos : getPositionsInBox(box)) {
      BlockState state = level.getBlockState(pos);
      pos = pos.immutable();
      if (!state.is(Blocks.WATER) || random && level.getRandom().nextInt(128) != 0) {
        if (Block.isFaceFull(state.getCollisionShape(level, pos.below()), Direction.UP)) {
          BlockPos up = pos.above();
          BlockState stateUp = level.getBlockState(up);
          BlockState newState = null;
          if (!stateUp.isAir() || random && level.getRandom().nextInt(128) != 0) {
            if (stateUp.is(Blocks.SNOW)
                && (Integer) stateUp.getValue(SnowLayerBlock.LAYERS) < 8
                && level.getRandom().nextInt(512) == 0) {
              newState =
                  (BlockState)
                      stateUp.setValue(
                          SnowLayerBlock.LAYERS,
                          (Integer) stateUp.getValue(SnowLayerBlock.LAYERS) + 1);
            }
          } else {
            newState = Blocks.SNOW.defaultBlockState();
          }

          if (newState != null) {
            if (player != null) {
              PlayerHelper.checkedReplaceBlock((ServerPlayer) player, level, up, newState);
            } else {
              level.setBlockAndUpdate(up, newState);
            }
          }
        }
      } else if (player != null) {
        PlayerHelper.checkedReplaceBlock(
            (ServerPlayer) player, level, pos, Blocks.ICE.defaultBlockState());
      } else {
        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
      }
    }
  }

  public static boolean isLiquidContainerForFluid(
      @Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    return state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer
        && liquidBlockContainer.canPlaceLiquid(player, level, pos, state, fluid);
  }

  public static void placeFluid(
      @Nullable Player player,
      Level level,
      BlockPos pos,
      Direction sideHit,
      FlowingFluid fluid,
      boolean checkWaterVaporize) {
    if (isLiquidContainerForFluid(player, level, pos, level.getBlockState(pos), fluid)) {
      placeFluid(player, level, pos, fluid, checkWaterVaporize);
    } else {
      placeFluid(player, level, pos.relative(sideHit), fluid, checkWaterVaporize);
    }
  }

  public static void placeFluid(
      @Nullable Player player,
      Level level,
      BlockPos pos,
      FlowingFluid fluid,
      boolean checkWaterVaporize) {
    BlockState blockState = level.getBlockState(pos);
    if (checkWaterVaporize
        && (Boolean)
            level.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
        && fluid.is(FluidTags.WATER)) {
      level.playSound(
          null,
          pos,
          SoundEvents.FIRE_EXTINGUISH,
          SoundSource.PLAYERS,
          0.5F,
          2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);

      for (int l = 0; l < 8; l++) {
        level.addParticle(
            ParticleTypes.LARGE_SMOKE,
            pos.getX() + Math.random(),
            pos.getY() + Math.random(),
            pos.getZ() + Math.random(),
            0.0,
            0.0,
            0.0);
      }
    } else if (isLiquidContainerForFluid(player, level, pos, blockState, fluid)) {
      ((LiquidBlockContainer) blockState.getBlock())
          .placeLiquid(level, pos, blockState, fluid.getSource(false));
      level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    } else {
      if (blockState.canBeReplaced(fluid) && !blockState.liquid()) {
        level.destroyBlock(pos, true);
      }

      if (player == null) {
        level.setBlockAndUpdate(pos, fluid.defaultFluidState().createLegacyBlock());
        level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
      } else if (PlayerHelper.checkedPlaceBlock(
          player, level, pos, fluid.defaultFluidState().createLegacyBlock())) {
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
      }
    }
  }

  public static void copySignData(Level level, BlockPos pos, SignBlockEntity oldSign) {
    if (oldSign != null && level.getBlockEntity(pos) instanceof SignBlockEntity newSign) {
      newSign.setText(oldSign.getText(true), true);
      newSign.setText(oldSign.getText(false), false);
      newSign.setAllowedPlayerEditor(oldSign.getPlayerWhoMayEdit());
      newSign.setWaxed(oldSign.isWaxed());
    }
  }

  public static AABB getBroadDeepBox(BlockPos pos, Direction direction, int offset) {
    return getBroadDeepBox(pos, direction, offset, 2 * offset);
  }

  public static AABB getBroadDeepBox(BlockPos pos, Direction direction, int breadth, int depth) {
    AABB box = getBroadBox(pos, direction, breadth);
    return depth == 0
        ? box
        : box.expandTowards(
            depth * -direction.getStepX(),
            depth * -direction.getStepY(),
            depth * -direction.getStepZ());
  }

  public static AABB getDeepBox(BlockPos pos, Direction direction, int depth) {
    return getBroadDeepBox(pos, direction, 1, depth);
  }

  public static AABB getBroadBox(BlockPos pos, Direction direction, int breadth) {
    AABB point = new AABB(pos);
    if (breadth == 0) {
      return point;
    }

    return switch (direction) {
      case EAST, WEST -> point.inflate(0.0, breadth, breadth);
      case UP, DOWN -> point.inflate(breadth, 0.0, breadth);
      case SOUTH, NORTH -> point.inflate(breadth, breadth, 0.0);
      default -> throw new MatchException(null, null);
    };
  }

  public static AABB getFlatYBox(BlockPos pos, int offset) {
    return getBroadBox(pos, Direction.UP, offset);
  }

  public static Iterable<BlockPos> getPositionsInBox(AABB box) {
    float epsilon = 1.0E-6F;
    return BlockPos.betweenClosed(
        Mth.floor(box.minX + epsilon),
        Mth.floor(box.minY + epsilon),
        Mth.floor(box.minZ + epsilon),
        Mth.floor(box.maxX - epsilon),
        Mth.floor(box.maxY - epsilon),
        Mth.floor(box.maxZ - epsilon));
  }

  public static Iterable<BlockPos> horizontalPositionsAround(BlockPos pos, int horizontalRadius) {
    return positionsAround(pos, horizontalRadius, 0, horizontalRadius);
  }

  public static Iterable<BlockPos> positionsAround(BlockPos pos, int radius) {
    return positionsAround(pos, radius, radius, radius);
  }

  public static Iterable<BlockPos> positionsAround(
      BlockPos pos, int xRadius, int yRadius, int zRadius) {
    return BlockPos.betweenClosed(
        pos.offset(-xRadius, -yRadius, -zRadius), pos.offset(xRadius, yRadius, zRadius));
  }

  public static List<BlockEntity> getBlockEntitiesWithinAABB(
      Level level, AABB box, Predicate<BlockEntity> predicate) {
    List<BlockEntity> list = new ArrayList<>();

    for (BlockPos pos : getPositionsInBox(box)) {
      BlockEntity blockEntity = getBlockEntity(level, pos);
      if (blockEntity != null && predicate.test(blockEntity)) {
        list.add(blockEntity);
      }
    }

    return list;
  }

  public static void gravitateEntityTowards(Entity ent, Vec3 target) {
    Vec3 difference = target.subtract(ent.position());
    double vel = 1.0 - difference.length() / 15.0;
    if (vel > 0.0) {
      vel *= vel;
      ent.addDeltaMovement(difference.normalize().scale(vel).multiply(0.1, 0.2, 0.1));
    }
  }

  public static void growNearbyRandomly(boolean harvest, Level level, Player player) {
    growNearbyRandomly(harvest, level, player.getBoundingBox().inflate(5.0, 3.0, 5.0), player);
  }

  public static void growNearbyRandomly(
      boolean harvest, Level level, AABB box, @Nullable Player player) {
    if (level instanceof ServerLevel serverLevel) {
      boolean grewWater = false;
      int chance = harvest ? 16 : 32;

      for (BlockPos currentPos : getPositionsInBox(box)) {
        currentPos = currentPos.immutable();
        BlockState state = level.getBlockState(currentPos);
        if (state.getBlock() instanceof BonemealableBlock growable) {
          if (growable.isValidBonemealTarget(level, currentPos, state)) {
            if ((ProjectEConfig.server.items.harvBandIndirect.get()
                    || !onlyAffectsOtherBlocks(state.getBlock()))
                && level.getRandom().nextInt(chance) == 0
                && growable.isBonemealSuccess(level, level.getRandom(), currentPos, state)) {
              growable.performBonemeal(serverLevel, level.getRandom(), currentPos, state);
              level.levelEvent(1505, currentPos, 0);
            }
          } else {
            tryHarvest(level, currentPos, state, player, harvest);
          }
        } else if (!isPlantable(state)) {
          if (!grewWater
              && level.getRandom().nextInt(512) == 0
              && BoneMealItem.growWaterPlant(ItemStack.EMPTY, level, currentPos, null)) {
            level.levelEvent(1505, currentPos, 0);
            grewWater = true;
          }
        } else {
          if (state.isRandomlyTicking() && level.getRandom().nextInt(chance / 4) == 0) {
            Block initialType = state.getBlock();
            int i = 0;

            for (int ticks = harvest ? 8 : 4; i < ticks; i++) {
              state.randomTick(serverLevel, currentPos, level.getRandom());
              state = level.getBlockState(currentPos);
              if (!state.is(initialType)) {
                break;
              }
            }

            if (!state.is(initialType)) {
              continue;
            }
          }

          tryHarvest(level, currentPos, state, player, harvest);
        }
      }
    }
  }

  private static void tryHarvest(
      Level level, BlockPos pos, BlockState state, @Nullable Player player, boolean harvest) {
    if (harvest
        && shouldHarvest(level, pos, state)
        && (player == null || PlayerHelper.hasEditPermission(player, level, pos))
        && !(player instanceof ServerPlayer serverPlayer
            && !PlayerHelper.checkBreakPermission(serverPlayer, level, pos))) {
      level.destroyBlock(pos, true, player);
    }
  }

  private static boolean shouldHarvest(Level level, BlockPos pos, BlockState state) {
    Block block = state.getBlock();
    if (state.is(PETags.Blocks.BLACKLIST_HARVEST) || isUnharvestableImplementation(block)) {
      return false;
    } else if (block instanceof BambooStalkBlock || block instanceof SugarCaneBlock) {
      return level.getBlockState(pos.below()).is(block);
    } else if (block instanceof CactusBlock) {
      return !level.getBlockState(pos.below()).is(block)
          && level.getBlockState(pos.above()).is(block);
    } else if (block instanceof GrowingPlantBlock) {
      return level.getBlockState(pos.above()).is(block)
          || level.getBlockState(pos.below()).is(block);
    } else if (block instanceof LeavesBlock leavesBlock) {
      return !(Boolean) state.getValue(LeavesBlock.PERSISTENT)
          && LeavesBlock.getOptionalDistanceAt(state).orElse(7) >= 7;
    } else {
      if (block instanceof VineBlock) {
        return level.getBlockState(pos.above()).is(block);
      }

      if (block instanceof GlowLichenBlock) {}

      if (block instanceof CropBlock cropBlock) {
        return cropBlock.isMaxAge(state);
      } else {
        IntegerProperty ageProperty = null;
        if (AGE_PROPERTIES.containsKey(block)) {
          ageProperty = AGE_PROPERTIES.get(block);
        } else {
          for (Value<?> value : state.getValues().toList()) {
            Property<?> property = value.property();
            if (property.getName().equals("age")) {
              if (property instanceof IntegerProperty intProperty) {
                ageProperty = intProperty;
              }
              break;
            }
          }

          AGE_PROPERTIES.put(block, ageProperty);
        }

        return ageProperty == null
            || ((Integer) state.getValue(ageProperty))
                .equals(ageProperty.getPossibleValues().getLast());
      }
    }
  }

  public static boolean isUnharvestableImplementation(Block block) {
    return block instanceof StemBlock
        || block instanceof AttachedStemBlock
        || block instanceof LilyPadBlock
        || onlyAffectsOtherBlocks(block);
  }

  private static boolean isPlantable(BlockState state) {
    return state.is(PETags.Blocks.OVERRIDE_PLANTABLE)
        || isPlantableImplementation(state.getBlock());
  }

  public static boolean isPlantableImplementation(Block block) {
    return block instanceof CactusBlock
        || block instanceof SugarCaneBlock
        || block instanceof VineBlock
        || block instanceof BushBlock;
  }

  public static boolean isCrop(BlockState state) {
    return !(state.getBlock() instanceof BonemealableBlock)
        ? isPlantable(state)
        : ProjectEConfig.server.items.harvBandIndirect.get()
            || !onlyAffectsOtherBlocks(state.getBlock());
  }

  private static boolean onlyAffectsOtherBlocks(Block block) {
    return block instanceof GrassBlock
        || block instanceof NyliumBlock
        || block == Blocks.MOSS_BLOCK
        || block == Blocks.PALE_MOSS_BLOCK;
  }

  private static <DATA> boolean validState(
      DATA data,
      BiPredicate<BlockState, DATA> stateChecker,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player) {
    return stateChecker.test(state, data)
        && state.getDestroySpeed(level, pos) != -1.0F
        && PlayerHelper.hasEditPermission(player, level, pos);
  }

  public static <DATA> int harvestVein(
      Level level,
      Player player,
      ItemStack stack,
      AABB area,
      List<ItemStack> currentDrops,
      DATA data,
      BiPredicate<BlockState, DATA> stateChecker) {
    int numMined = 0;
    Set<BlockPos> traversed = new HashSet<>();

    record TargetInfo(BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
      public static TargetInfo create(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity =
            state.hasBlockEntity() ? WorldHelper.getBlockEntity(level, pos) : null;
        return new TargetInfo(
            pos, state.getBlock().playerWillDestroy(level, pos, state, player), blockEntity);
      }
    }

    Queue<TargetInfo> frontier = new ArrayDeque<>();
    WorldHelper.VeinStateChecker<DATA> validState;
    if (level.isClientSide()) {
      validState = WorldHelper::validState;
    } else {
      validState =
          (dat, checker, statex, lvl, posx, p) ->
              validState(dat, checker, statex, lvl, posx, p)
                  && PlayerHelper.checkBreakPermission((ServerPlayer) p, lvl, posx);
    }

    for (BlockPos pos : getPositionsInBox(area)) {
      BlockState state = level.getBlockState(pos);
      if (validState.test(data, stateChecker, state, level, pos, player)) {
        if (level.isClientSide()) {
          return 1;
        }

        pos = pos.immutable();
        frontier.add(TargetInfo.create(level, pos, state, player));
      }

      traversed.add(pos.immutable());
    }

    while (!frontier.isEmpty()) {
      TargetInfo targetInfo = frontier.poll();
      BlockPos pos = targetInfo.pos();
      BlockState state = targetInfo.state();
      Block block = state.getBlock();
      if (level.destroyBlock(pos, false, player, 512)) {
        block.destroy(level, pos, state);
        player.awardStat(Stats.BLOCK_MINED.get(block));
        collectBlockDrops(
            state, (ServerLevel) level, pos, targetInfo.blockEntity(), player, stack, currentDrops);
        if (++numMined >= 250) {
          break;
        }

        for (BlockPos nextPos : positionsAround(pos, 1)) {
          nextPos = nextPos.immutable();
          if (traversed.add(nextPos) && isBlockLoaded(level, nextPos)) {
            BlockState nextState = level.getBlockState(nextPos);
            if (validState.test(data, stateChecker, nextState, level, nextPos, player)) {
              frontier.add(TargetInfo.create(level, nextPos, nextState, player));
            }
          }
        }
      }
    }

    return numMined;
  }

  public static void igniteNearby(Level level, Player player) {
    for (BlockPos pos : getPositionsInBox(player.getBoundingBox().inflate(8.0, 5.0, 8.0))) {
      if (level.getRandom().nextInt(128) == 0 && level.isEmptyBlock(pos)) {
        PlayerHelper.checkedPlaceBlock(
            player, level, pos.immutable(), Blocks.FIRE.defaultBlockState());
      }
    }
  }

  public static boolean validRepelEntity(Entity entity, TagKey<EntityType<?>> blacklistTag) {
    if (entity.isSpectator() || entity.getType().builtInRegistryHolder().is(blacklistTag)) {
      return false;
    } else {
      return entity instanceof Projectile ? !entity.onGround() : entity instanceof Mob;
    }
  }

  public static void repelEntitiesSWRG(Level level, AABB effectBounds, Player player) {
    Vec3 playerVec = player.position();

    for (Entity ent : level.getEntitiesOfClass(Entity.class, effectBounds, SWRG_REPEL_PREDICATE)) {
      if (ent instanceof Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (level.isClientSide() && owner == null
            || owner != null && player.getUUID().equals(owner.getUUID())) {
          continue;
        }
      }

      repelEntity(playerVec, ent);
    }
  }

  public static void repelEntity(Vec3 vec, Entity entity) {
    double distance = vec.distanceTo(entity.position()) + 0.1;
    entity.push(entity.position().subtract(vec).scale(1.0 / (1.5 * distance)));
  }

  @NotNull
  public static InteractionResult igniteBlock(UseOnContext ctx) {
    Player player = ctx.getPlayer();
    if (player == null) {
      return InteractionResult.FAIL;
    }

    Level level = ctx.getLevel();
    BlockPos pos = ctx.getClickedPos();
    Direction side = ctx.getClickedFace();
    if (BaseFireBlock.canBePlacedAt(level, pos, side)) {
      if (!level.isClientSide()
          && PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
        level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            PESoundEvents.POWER.get(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F);
      }
    } else {
      BlockState state = level.getBlockState(pos);
      BlockState modifiedState =
          ToolActionHelper.getToolModifiedState(state, ctx, ItemAbilities.FIRESTARTER_LIGHT, true);
      if (modifiedState != null) {
        if (!level.isClientSide()
            && PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
          BlockState actualModifiedState =
              ToolActionHelper.getToolModifiedState(
                  state, ctx, ItemAbilities.FIRESTARTER_LIGHT, false);
          if (actualModifiedState != null) {
            level.setBlockAndUpdate(pos, actualModifiedState);
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                PESoundEvents.POWER.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
          }
        }
      } else if (state.getBlock() instanceof TntBlock) {
        if (!level.isClientSide()
            && PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
          TntBlock.prime(level, pos);
          level.playSound(
              null,
              player.getX(),
              player.getY(),
              player.getZ(),
              PESoundEvents.POWER.get(),
              SoundSource.PLAYERS,
              1.0F,
              1.0F);
        }
      } else {
        Entry entry = (Entry) FlammableBlockRegistry.getDefaultInstance().get(state.getBlock());
        boolean flammable = state.ignitedByLava() || entry != null && entry.getIgniteOdds() > 0;
        if (!flammable) {
          return InteractionResult.PASS;
        }

        BlockPos firePos = pos.relative(side);
        if (!BaseFireBlock.canBePlacedAt(level, firePos, ctx.getHorizontalDirection())) {
          return InteractionResult.PASS;
        }

        if (!level.isClientSide()
            && PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
          level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
          level.playSound(
              null,
              player.getX(),
              player.getY(),
              player.getZ(),
              PESoundEvents.POWER.get(),
              SoundSource.PLAYERS,
              1.0F,
              1.0F);
        }
      }
    }

    return PESidedSuccess.sidedSuccess(level.isClientSide());
  }

  @Contract("null, _ -> false")
  public static boolean isChunkLoaded(@Nullable LevelReader world, @NotNull BlockPos pos) {
    return isChunkLoaded(
        world,
        SectionPos.blockToSectionCoord(pos.getX()),
        SectionPos.blockToSectionCoord(pos.getZ()));
  }

  @Contract("null, _ -> false")
  public static boolean isChunkLoaded(@Nullable LevelReader world, ChunkPos chunkPos) {
    return isChunkLoaded(world, chunkPos.getMinBlockX() >> 4, chunkPos.getMinBlockZ() >> 4);
  }

  @Contract("null, _, _ -> false")
  public static boolean isChunkLoaded(@Nullable LevelReader world, int chunkX, int chunkZ) {
    if (world == null) {
      return false;
    } else {
      return world instanceof LevelAccessor accessor
          ? accessor.hasChunk(chunkX, chunkZ)
          : world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }
  }

  public static boolean isBlockLoaded(@Nullable BlockGetter world, @NotNull BlockPos pos) {
    if (world == null) {
      return false;
    } else if (world instanceof LevelReader reader) {
      return reader instanceof Level level && !level.isInWorldBounds(pos)
          ? false
          : isChunkLoaded(reader, pos);
    } else {
      return true;
    }
  }

  @Nullable
  @Contract("null, _, _, _ -> null")
  public static <CAP, CONTEXT> CAP getCapability(
      @Nullable Level level, BlockCapability<CAP, CONTEXT> cap, BlockPos pos, CONTEXT context) {
    return getCapability(level, cap, pos, null, null, context);
  }

  @Nullable
  @Contract("null, _, _, _, _, _ -> null")
  public static <CAP, CONTEXT> CAP getCapability(
      @Nullable Level level,
      BlockCapability<CAP, CONTEXT> cap,
      BlockPos pos,
      @Nullable BlockState state,
      @Nullable BlockEntity blockEntity,
      CONTEXT context) {
    return !isBlockLoaded(level, pos) ? null : cap.find(level, pos, context);
  }

  @Nullable
  public static BlockEntity getBlockEntity(@Nullable BlockGetter level, @NotNull BlockPos pos) {
    return !isBlockLoaded(level, pos) ? null : level.getBlockEntity(pos);
  }

  @Nullable
  public static <BE extends BlockEntity> BE getBlockEntity(
      @NotNull Class<BE> clazz, @Nullable BlockGetter level, @NotNull BlockPos pos) {
    return getBlockEntity(clazz, level, pos, false);
  }

  @Nullable
  public static <BE extends BlockEntity> BE getBlockEntity(
      @NotNull Class<BE> clazz,
      @Nullable BlockGetter level,
      @NotNull BlockPos pos,
      boolean logWrongType) {
    BlockEntity blockEntity = getBlockEntity(level, pos);
    if (blockEntity == null) {
      return null;
    }

    if (clazz.isInstance(blockEntity)) {
      return clazz.cast(blockEntity);
    }

    if (logWrongType) {
      PECore.LOGGER.warn(
          "Unexpected block entity class at {}, expected {}, but found: {}",
          new Object[] {pos, clazz, blockEntity.getClass()});
    }

    return null;
  }

  @FunctionalInterface
  private interface VeinStateChecker<DATA> {
    boolean test(
        DATA var1,
        BiPredicate<BlockState, DATA> var2,
        BlockState var3,
        Level var4,
        BlockPos var5,
        Player var6);
  }
}
