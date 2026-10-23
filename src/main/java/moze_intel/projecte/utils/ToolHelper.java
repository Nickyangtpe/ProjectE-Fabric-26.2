package moze_intel.projecte.utils;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.compat.common.IShearable;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.Tags;
import moze_intel.projecte.compat.common.ToolActionHelper;
import moze_intel.projecte.compat.event.ItemAttributeModifierEvent;
import moze_intel.projecte.compat.util.PESidedSuccess;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.items.tools.PEPickaxe;
import moze_intel.projecte.gameObjs.registries.PEDamageTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ToolHelper {
  private static final Identifier CHARGE_MODIFIER_ID = PECore.rl("charge_modifier");
  public static final ItemAbility HAMMER_DIG = ItemAbility.get("hammer_dig");
  public static final ItemAbility KATAR_DIG = ItemAbility.get("katar_dig");
  public static final ItemAbility MORNING_STAR_DIG = ItemAbility.get("morning_star_dig");
  public static final Set<ItemAbility> DEFAULT_PE_HAMMER_ACTIONS = of(HAMMER_DIG);
  public static final Set<ItemAbility> DEFAULT_PE_KATAR_ACTIONS = of(KATAR_DIG);
  public static final Set<ItemAbility> DEFAULT_PE_MORNING_STAR_ACTIONS = of(MORNING_STAR_DIG);
  private static final Predicate<Entity> SHEARABLE =
      entity ->
          !entity.isSpectator()
              && (entity instanceof IShearable
                  || entity instanceof Shearable shearable && shearable.readyForShearing());
  private static final Predicate<Entity> SLAY_MOB =
      entity -> !entity.isSpectator() && entity instanceof Enemy;
  private static final Predicate<Entity> SLAY_ALL =
      entity ->
          !entity.isSpectator() && (entity instanceof Enemy || entity instanceof LivingEntity);

  private static Set<ItemAbility> of(ItemAbility... actions) {
    return Stream.of(actions).collect(Collectors.toCollection(Sets::newIdentityHashSet));
  }

  @SafeVarargs
  public static InteractionResult performActions(
      UseOnContext context,
      BlockState state,
      InteractionResult firstAction,
      BiFunction<UseOnContext, BlockState, InteractionResult>... secondaryActions) {
    if (firstAction.consumesAction()) {
      return firstAction;
    }

    InteractionResult result = firstAction;
    boolean hasFailed = result == InteractionResult.FAIL;

    for (BiFunction<UseOnContext, BlockState, InteractionResult> secondaryAction :
        secondaryActions) {
      result = secondaryAction.apply(context, state);
      if (result.consumesAction()) {
        return result;
      }

      hasFailed &= result == InteractionResult.FAIL;
    }

    return (InteractionResult) (hasFailed ? InteractionResult.FAIL : InteractionResult.PASS);
  }

  public static InteractionResult clearTagAOE(
      Level level,
      Player player,
      InteractionHand hand,
      ItemStack stack,
      long emcCost,
      TagKey<Block> tag) {
    if (ProjectEConfig.server.items.disableAllRadiusMining.get()) {
      return InteractionResult.PASS;
    }

    int charge = getCharge(stack);
    if (charge == 0) {
      return InteractionResult.PASS;
    }

    int horizontalRadius = 5 * charge;
    int verticalRadius = 2 * horizontalRadius;
    boolean hasAction = false;
    List<ItemStack> drops = new ArrayList<>();
    Vec3 dropPosition = player.position();

    for (BlockPos pos :
        WorldHelper.getPositionsInBox(
            player.getBoundingBox().inflate(horizontalRadius, verticalRadius, horizontalRadius))) {
      BlockState state = level.getBlockState(pos);
      if (state.is(tag)) {
        if (level.isClientSide()) {
          return InteractionResult.SUCCESS;
        }

        pos = pos.immutable();
        if (PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
          if (!ItemPE.consumeFuel(player, stack, emcCost, true)) {
            break;
          }

          BlockEntity blockEntity = WorldHelper.getBlockEntity(level, pos);
          if (!level.removeBlock(pos, false)) {
            continue;
          }
          WorldHelper.collectBlockDrops(
              state, (ServerLevel) level, pos, blockEntity, player, stack, drops, dropPosition);
          hasAction = true;
          if (level.getRandom().nextInt(5) == 0) {
            ((ServerLevel) level)
                .sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    2,
                    0.0,
                    0.0,
                    0.0,
                    0.0);
          }
        }
      }
    }

    if (hasAction) {
      WorldHelper.createLootDrop(drops, level, dropPosition);
      return InteractionResult.SUCCESS;
    } else {
      return InteractionResult.PASS;
    }
  }

  public static InteractionResult dowseCampfire(UseOnContext context, BlockState state) {
    Player player = context.getPlayer();
    if (player == null) {
      return InteractionResult.PASS;
    }

    if (state.getBlock() instanceof CampfireBlock && (Boolean) state.getValue(CampfireBlock.LIT)) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if (!level.isClientSide()) {
        level.levelEvent(1009, pos, 0);
      }

      CampfireBlock.dowse(player, level, pos, state);
      if (!level.isClientSide()) {
        level.setBlock(pos, (BlockState) state.setValue(CampfireBlock.LIT, Boolean.FALSE), 11);
      }

      return PESidedSuccess.sidedSuccess(level.isClientSide());
    } else {
      return InteractionResult.PASS;
    }
  }

  public static InteractionResult tillAOE(
      UseOnContext context, BlockState clickedState, long emcCost) {
    return useAOE(
        context,
        clickedState,
        emcCost,
        ItemAbilities.HOE_TILL,
        SoundEvents.HOE_TILL,
        -1,
        new ToolHelper.HoeToolAOEData());
  }

  public static InteractionResult flattenAOE(
      UseOnContext context, BlockState clickedState, long emcCost) {
    Direction sideHit = context.getClickedFace();
    return (InteractionResult)
        (sideHit == Direction.DOWN
            ? InteractionResult.PASS
            : useAOE(
                context,
                clickedState,
                emcCost,
                ItemAbilities.SHOVEL_FLATTEN,
                SoundEvents.SHOVEL_FLATTEN,
                -1,
                new ToolHelper.ShovelToolAOEData()));
  }

  public static InteractionResult stripLogsAOE(
      UseOnContext context, BlockState clickedState, long emcCost) {
    return useAxeAOE(
        context, clickedState, emcCost, ItemAbilities.AXE_STRIP, SoundEvents.AXE_STRIP, -1);
  }

  public static InteractionResult scrapeAOE(
      UseOnContext context, BlockState clickedState, long emcCost) {
    return useAxeAOE(
        context, clickedState, emcCost, ItemAbilities.AXE_SCRAPE, SoundEvents.AXE_SCRAPE, 3005);
  }

  public static InteractionResult waxOffAOE(
      UseOnContext context, BlockState clickedState, long emcCost) {
    return useAxeAOE(
        context, clickedState, emcCost, ItemAbilities.AXE_WAX_OFF, SoundEvents.AXE_WAX_OFF, 3004);
  }

  private static InteractionResult useAxeAOE(
      UseOnContext context,
      BlockState clickedState,
      long emcCost,
      ItemAbility action,
      SoundEvent sound,
      int particle) {
    return useAOE(
        context, clickedState, emcCost, action, sound, particle, new ToolHelper.AxeToolAOEData());
  }

  private static InteractionResult useAOE(
      UseOnContext context,
      BlockState clickedState,
      long emcCost,
      ItemAbility action,
      SoundEvent sound,
      int particle,
      ToolHelper.IToolAOEData toolAOEData) {
    Player player = context.getPlayer();
    if (player == null) {
      return InteractionResult.PASS;
    }

    Level level = context.getLevel();
    BlockPos pos = context.getClickedPos();
    if (!toolAOEData.isValid(level, pos, clickedState)) {
      return InteractionResult.PASS;
    }

    BlockState modifiedState =
        ToolActionHelper.getToolModifiedState(clickedState, context, action, false);
    if (modifiedState == null) {
      return InteractionResult.PASS;
    }

    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(
        (ServerPlayer) player, pos, context.getItemInHand());
    level.setBlock(pos, modifiedState, 11);
    level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    if (particle != -1) {
      level.levelEvent(particle, pos, 0);
    }

    ItemStack stack = context.getItemInHand();
    int charge = getCharge(stack);
    if (charge > 0) {
      Direction side = context.getClickedFace();
      toolAOEData.persistData(level, pos, clickedState, side);

      for (BlockPos newPos : toolAOEData.getTargetPositions(pos, side, charge)) {
        if (!pos.equals(newPos)) {
          BlockState state = level.getBlockState(newPos);
          UseOnContext adjustedContext =
              new UseOnContext(
                  level,
                  context.getPlayer(),
                  context.getHand(),
                  context.getItemInHand(),
                  new BlockHitResult(
                      context
                          .getClickLocation()
                          .add(
                              newPos.getX() - pos.getX(),
                              newPos.getY() - pos.getY(),
                              newPos.getZ() - pos.getZ()),
                      context.getClickedFace(),
                      newPos,
                      context.isInside()));
          if (toolAOEData.isValid(level, newPos, state)
              && modifiedState
                  == ToolActionHelper.getToolModifiedState(state, adjustedContext, action, true)) {
            if (!ItemPE.consumeFuel(player, stack, emcCost, true)) {
              break;
            }

            newPos = newPos.immutable();
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(
                (ServerPlayer) player, newPos, context.getItemInHand());
            ToolActionHelper.getToolModifiedState(state, adjustedContext, action, false);
            level.setBlock(newPos, modifiedState, 11);
            if (particle != -1) {
              level.levelEvent(particle, newPos, 0);
            }
          }
        }
      }
    }

    level.playSound(
        null,
        player.getX(),
        player.getY(),
        player.getZ(),
        PESoundEvents.CHARGE.get(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
    return InteractionResult.CONSUME;
  }

  public static void digBasedOnMode(
      ItemStack stack,
      Level level,
      BlockPos pos,
      LivingEntity living,
      ToolHelper.RayTracePointer tracePointer,
      PEPickaxe.PickaxeMode mode) {
    if (!level.isClientSide()
        && mode != PEPickaxe.PickaxeMode.STANDARD
        && !ProjectEConfig.server.items.disableAllRadiusMining.get()
        && living instanceof Player player) {
      BlockHitResult result = tracePointer.rayTrace(level, player, Fluid.NONE);
      if (result.getType() != Type.MISS && pos.equals(result.getBlockPos())) {
        digTargetsBasedOnMode(stack, level, pos, player, result.getDirection(), mode, false);
      }
    }
  }

  public static void digBasedOnModeAfterPrimaryBreak(
      ItemStack stack,
      Level level,
      BlockPos pos,
      ServerPlayer player,
      Direction hitSide,
      PEPickaxe.PickaxeMode mode) {
    if (!level.isClientSide()
        && mode != PEPickaxe.PickaxeMode.STANDARD
        && !ProjectEConfig.server.items.disableAllRadiusMining.get()) {
      digTargetsBasedOnMode(stack, level, pos, player, hitSide, mode, true);
    }
  }

  private static void digTargetsBasedOnMode(
      ItemStack stack,
      Level level,
      BlockPos pos,
      Player player,
      Direction hitSide,
      PEPickaxe.PickaxeMode mode,
      boolean primaryAlreadyBroken) {
    List<ItemStack> drops = new ArrayList<>();
    Vec3 dropPosition = Vec3.atLowerCornerOf(pos);

    for (BlockPos digPos : getTargets(pos, player, hitSide, mode)) {
      if (!primaryAlreadyBroken || !pos.equals(digPos)) {
        BlockState state = level.getBlockState(digPos);
        if (!state.isAir()
            && state.getDestroySpeed(level, digPos) != -1.0F
            && stack.getItem().isCorrectToolForDrops(stack, state)) {
          digPos = digPos.immutable();
          if (PlayerHelper.hasBreakPermission((ServerPlayer) player, level, digPos)) {
            BlockEntity blockEntity = WorldHelper.getBlockEntity(level, digPos);
            if (level.removeBlock(digPos, false)) {
              WorldHelper.collectBlockDrops(
                  state, (ServerLevel) level, digPos, blockEntity, player, stack, drops,
                  dropPosition);
            }
          }
        }
      }
    }

    WorldHelper.createLootDrop(drops, level, dropPosition);
  }

  private static Iterable<BlockPos> getTargets(
      BlockPos pos, Player player, Direction sideHit, PEPickaxe.PickaxeMode mode) {
    return switch (mode) {
      case TALLSHOT -> BlockPos.betweenClosed(pos.below(), pos.above());
      case WIDESHOT -> {
        switch (sideHit.getAxis() == Axis.Y ? player.getDirection().getAxis() : sideHit.getAxis()) {
          case X:
            yield BlockPos.betweenClosed(pos.south(), pos.north());
          case Z:
            yield BlockPos.betweenClosed(pos.west(), pos.east());
          default:
            yield Collections.singleton(pos);
        }
      }
      case LONGSHOT -> BlockPos.betweenClosed(pos, pos.relative(sideHit.getOpposite(), 2));
      default -> Collections.singleton(pos);
    };
  }

  public static InteractionResult digAOE(
      Level level,
      Player player,
      InteractionHand hand,
      ItemStack stack,
      BlockPos pos,
      Direction sideHit,
      boolean affectDepth,
      long emcCost) {
    if (ProjectEConfig.server.items.disableAllRadiusMining.get()) {
      return InteractionResult.PASS;
    }

    int charge = getCharge(stack);
    if (charge == 0) {
      return InteractionResult.PASS;
    }

    AABB box =
        affectDepth
            ? WorldHelper.getBroadDeepBox(pos, sideHit, charge)
            : WorldHelper.getFlatYBox(pos, charge);
    boolean hasAction = false;
    List<ItemStack> drops = new ArrayList<>();
    Vec3 dropPosition = Vec3.atLowerCornerOf(pos);

    for (BlockPos newPos : WorldHelper.getPositionsInBox(box)) {
      BlockState state = level.getBlockState(newPos);
      if (!state.isAir()
          && state.getDestroySpeed(level, newPos) != -1.0F
          && stack.getItem().isCorrectToolForDrops(stack, state)) {
        if (level.isClientSide()) {
          return InteractionResult.SUCCESS;
        }

        newPos = newPos.immutable();
        if (PlayerHelper.hasBreakPermission((ServerPlayer) player, level, newPos)) {
          if (!ItemPE.consumeFuel(player, stack, emcCost, true)) {
            break;
          }

          BlockEntity blockEntity = WorldHelper.getBlockEntity(level, newPos);
          if (!level.removeBlock(newPos, false)) {
            continue;
          }
          WorldHelper.collectBlockDrops(
              state, (ServerLevel) level, newPos, blockEntity, player, stack, drops, dropPosition);
          hasAction = true;
        }
      }
    }

    if (hasAction) {
      WorldHelper.createLootDrop(drops, level, dropPosition);
      player
          .level()
          .playSound(
              null,
              player.getX(),
              player.getY(),
              player.getZ(),
              PESoundEvents.DESTRUCT.get(),
              SoundSource.PLAYERS,
              1.0F,
              1.0F);
      return InteractionResult.SUCCESS;
    } else {
      return InteractionResult.PASS;
    }
  }

  public static void attackWithCharge(
      ItemStack stack, LivingEntity damaged, LivingEntity damager, float baseDmg) {
    if (damager instanceof Player player && !damager.level().isClientSide()) {
      int charge = getCharge(stack);
      float totalDmg = baseDmg;
      DamageSource dmg;
      if (charge > 0) {
        dmg = PEDamageTypes.BYPASS_ARMOR_PLAYER_ATTACK.source(player);
        totalDmg += charge;
      } else {
        dmg = damager.damageSources().playerAttack(player);
      }

      damaged.hurt(dmg, totalDmg);
    }
  }

  public static void attackAOE(
      ItemStack stack,
      Player player,
      boolean slayAll,
      float damage,
      long emcCost,
      InteractionHand hand) {
    Level level = player.level();
    if (!level.isClientSide()) {
      int charge = getCharge(stack);
      List<Entity> toAttack =
          level.getEntities(
              player,
              player.getBoundingBox().inflate(2.5F * charge),
              slayAll ? SLAY_ALL : SLAY_MOB);
      DamageSource src = PEDamageTypes.BYPASS_ARMOR_PLAYER_ATTACK.source(player);
      boolean hasAction = false;

      for (Entity entity : toAttack) {
        if (!ItemPE.consumeFuel(player, stack, emcCost, true)) {
          break;
        }

        entity.hurt(src, damage);
        hasAction = true;
      }

      if (hasAction) {
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            PESoundEvents.CHARGE.get(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F);
        PlayerHelper.swingItem(player, hand);
      }
    }
  }

  public static InteractionResult shearEntity(Player player, ItemStack stack, LivingEntity entity) {
    Level level = player.level();
    BlockPos pos = entity.blockPosition();
    if (entity instanceof IShearable target && target.isShearable(player, stack, level, pos)) {
      if (!level.isClientSide()) {
        for (ItemStack drop : target.onSheared(player, stack, level, pos)) {
          target.spawnShearedDrop(level, pos, drop);
        }

        entity.gameEvent(GameEvent.SHEAR, player);
      }

      return PESidedSuccess.sidedSuccess(level.isClientSide());
    } else if (entity instanceof Shearable target && target.readyForShearing()) {
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
        target.shear(serverLevel, SoundSource.PLAYERS, stack);
        entity.gameEvent(GameEvent.SHEAR, player);
      }

      return PESidedSuccess.sidedSuccess(level.isClientSide());
    } else {
      return InteractionResult.PASS;
    }
  }

  public static InteractionResult shearEntityAOE(
      Player player, InteractionHand hand, long emcCost) {
    Level level = player.level();
    ItemStack stack = player.getItemInHand(hand);
    int offset = (int) Math.pow(2.0, 2 + getCharge(stack));
    List<Entity> list =
        level.getEntitiesOfClass(
            Entity.class, player.getBoundingBox().inflate(offset, offset / 2.0, offset), SHEARABLE);
    boolean hasAction = false;
    List<ItemStack> drops = new ArrayList<>();

    for (Entity ent : list) {
      BlockPos entityPosition = ent.blockPosition();
      if (ent instanceof IShearable target
          && target.isShearable(player, stack, level, entityPosition)) {
        if (level.isClientSide()) {
          return InteractionResult.SUCCESS;
        }

        if (!ItemPE.consumeFuel(player, stack, emcCost, true)) {
          break;
        }

        List<ItemStack> entDrops = target.onSheared(player, stack, level, entityPosition);
        ent.gameEvent(GameEvent.SHEAR, player);
        if (!entDrops.isEmpty()) {
          drops.addAll(entDrops);
          drops.addAll(entDrops);
        }

        hasAction = true;
      } else if (ent instanceof Shearable target && target.readyForShearing()) {
        if (level.isClientSide()) {
          return InteractionResult.SUCCESS;
        }

        if (!ItemPE.consumeFuel(player, stack, emcCost, true)) {
          break;
        }

        if (level instanceof ServerLevel serverLevel) {
          target.shear(serverLevel, SoundSource.PLAYERS, stack);
          ent.gameEvent(GameEvent.SHEAR, player);
          hasAction = true;
        }
      }

      if (!level.isClientSide() && Math.random() < 0.01) {
        Entity e = ent.getType().create(level, EntitySpawnReason.EVENT);
        if (e != null) {
          e.setPos(ent.getX(), ent.getY(), ent.getZ());
          if (e instanceof Mob mob) {
            mob.finalizeSpawn(
                (ServerLevel) level,
                ((ServerLevel) level).getCurrentDifficultyAt(entityPosition),
                EntitySpawnReason.EVENT,
                null);
          }

          if (e instanceof Sheep sheep) {
            sheep.setColor(DyeColor.byId(level.getRandom().nextInt(16)));
          }

          if (e instanceof AgeableMob mob) {
            mob.setAge(-24000);
          }

          level.addFreshEntity(e);
        }
      }
    }

    if (hasAction) {
      WorldHelper.createLootDrop(drops, level, player.position());
      return InteractionResult.SUCCESS;
    } else {
      return InteractionResult.PASS;
    }
  }

  public static InteractionResult tryVeinMine(
      Player player, ItemStack stack, BlockPos pos, Direction sideHit) {
    if (ProjectEConfig.server.items.disableAllRadiusMining.get()) {
      return InteractionResult.PASS;
    } else {
      Level level = player.level();
      BlockState target = level.getBlockState(pos);
      if (target.getDestroySpeed(level, pos) != -1.0F
          && stack.getItem().isCorrectToolForDrops(stack, target)) {
        AABB area = WorldHelper.getBroadDeepBox(pos, sideHit, getCharge(stack));
        return harvestVein(
            level,
            player,
            pos,
            stack,
            area,
            target.getBlock(),
            TypedInstance::is,
            (drops, lvl, p) -> {
              WorldHelper.createLootDrop(drops, lvl, p);
              lvl.playSound(
                  null,
                  p.getX(),
                  p.getY(),
                  p.getZ(),
                  PESoundEvents.DESTRUCT.get(),
                  SoundSource.PLAYERS,
                  1.0F,
                  1.0F);
            });
      } else {
        return InteractionResult.FAIL;
      }
    }
  }

  public static InteractionResult mineOreVeinsInAOE(Player player, InteractionHand hand) {
    if (ProjectEConfig.server.items.disableAllRadiusMining.get()) {
      return InteractionResult.PASS;
    }

    Level level = player.level();
    ItemStack stack = player.getItemInHand(hand);
    BiPredicate<BlockState, ItemStack> stateChecker =
        (state, itemStack) ->
            state.is(Tags.Blocks.ORES)
                && itemStack.getItem().isCorrectToolForDrops(itemStack, state);
    AABB area = player.getBoundingBox().inflate(getCharge(stack) + 3);
    return harvestVein(
        level,
        player,
        player.blockPosition(),
        stack,
        area,
        stack,
        stateChecker,
        WorldHelper::createLootDrop);
  }

  public static <DATA> InteractionResult harvestVein(
      Level level,
      Player player,
      BlockPos dropPos,
      ItemStack stack,
      AABB area,
      DATA data,
      BiPredicate<BlockState, DATA> stateChecker,
      ToolHelper.DropSpawner spawnDrops) {
    if (ProjectEConfig.server.items.disableAllRadiusMining.get()) {
      return InteractionResult.PASS;
    }

    List<ItemStack> drops = new ArrayList<>();
    int mined;
    try (ExperienceDropTarget.Scope ignored =
        ExperienceDropTarget.at(level, Vec3.atLowerCornerOf(dropPos))) {
      mined = WorldHelper.harvestVein(level, player, stack, area, drops, data, stateChecker);
    }
    if (mined > 0) {
      if (!level.isClientSide()) {
        spawnDrops.drop(drops, level, dropPos);
      }

      return PESidedSuccess.sidedSuccess(level.isClientSide());
    } else {
      return InteractionResult.PASS;
    }
  }

  public static float getDestroySpeed(
      float parentDestroySpeed, IMatterType matterType, int charge) {
    return parentDestroySpeed == 1.0F
        ? parentDestroySpeed
        : parentDestroySpeed + matterType.getChargeModifier() * charge;
  }

  public static boolean canMatterMine(IMatterType matterType, Block block) {
    return block instanceof IMatterBlock matterBlock
        && matterBlock.getMatterType().getMatterTier() <= matterType.getMatterTier();
  }

  private static int getCharge(ItemStack stack) {
    IItemCharge charge = PECapabilities.CHARGE_ITEM_CAPABILITY.find(stack, null);
    return charge == null ? 0 : charge.getCharge(stack);
  }

  public static void applyChargeAttributes(ItemAttributeModifierEvent event) {
    int charge = getCharge(event.getItemStack());
    if (charge > 0) {
      event.addModifier(
          Attributes.ATTACK_DAMAGE,
          new AttributeModifier(CHARGE_MODIFIER_ID, charge, Operation.ADD_VALUE),
          EquipmentSlotGroup.MAINHAND);
    }
  }

  private static class AxeToolAOEData implements ToolHelper.IToolAOEData {
    @Nullable private Axis axis;
    private boolean isSet;

    @Override
    public boolean isValid(Level level, BlockPos blockPos, BlockState state) {
      return !this.isSet || this.axis == this.getAxis(state);
    }

    @Override
    public void persistData(Level level, BlockPos pos, BlockState state, Direction side) {
      this.axis = this.getAxis(state);
      this.isSet = true;
    }

    @Override
    public Iterable<BlockPos> getTargetPositions(BlockPos pos, Direction side, int radius) {
      return WorldHelper.getPositionsInBox(WorldHelper.getBroadBox(pos, side, radius));
    }

    @Nullable
    private Axis getAxis(BlockState state) {
      return state.hasProperty(RotatedPillarBlock.AXIS)
          ? (Axis) state.getValue(RotatedPillarBlock.AXIS)
          : null;
    }
  }

  @FunctionalInterface
  public interface DropSpawner {
    void drop(List<ItemStack> var1, Level var2, BlockPos var3);
  }

  private abstract static class FlatToolAOEData implements ToolHelper.IToolAOEData {
    @Override
    public Iterable<BlockPos> getTargetPositions(BlockPos pos, Direction side, int radius) {
      return WorldHelper.horizontalPositionsAround(pos, radius);
    }
  }

  private static class HoeToolAOEData extends ToolHelper.FlatToolAOEData {
    @Override
    public boolean isValid(Level level, BlockPos pos, BlockState state) {
      return true;
    }
  }

  private interface IToolAOEData {
    boolean isValid(Level var1, BlockPos var2, BlockState var3);

    default void persistData(Level level, BlockPos pos, BlockState state, Direction side) {}

    Iterable<BlockPos> getTargetPositions(BlockPos var1, Direction var2, int var3);
  }

  @FunctionalInterface
  public interface RayTracePointer {
    BlockHitResult rayTrace(Level var1, Player var2, Fluid var3);
  }

  private static class ShovelToolAOEData extends ToolHelper.FlatToolAOEData {
    @Override
    public boolean isValid(Level level, BlockPos pos, BlockState state) {
      BlockPos abovePos = pos.above();
      BlockState aboveState = level.getBlockState(abovePos);
      if (aboveState.isAir()) {
        return true;
      } else {
        return aboveState.is(PETags.Blocks.FARMING_OVERRIDE)
                || aboveState.canBeReplaced() && aboveState.is(BlockTags.REPLACEABLE_BY_TREES)
            ? aboveState.getFluidState().isEmpty() && !aboveState.isSolidRender()
            : false;
      }
    }
  }
}
