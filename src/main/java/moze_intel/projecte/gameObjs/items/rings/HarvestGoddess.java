package moze_intel.projecte.gameObjs.items.rings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalRangeItem;
import moze_intel.projecte.compat.common.SpecialPlantable;
import moze_intel.projecte.compat.event.EventHooks;
import moze_intel.projecte.compat.event.entity.player.BonemealEvent;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.mixin.BlockPlaceContextAccessor;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HarvestGoddess extends PEToggleItem implements IPedestalRangeItem {
  public HarvestGoddess(Properties props) {
    super(
        props
            .component(PEDataComponentTypes.STORED_EMC, 0L)
            .component(PEDataComponentTypes.UNPROCESSED_EMC, 0.0));
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (!level.isClientSide()
        && hotBarOrOffHand(stack, entity, slot)
        && entity instanceof Player player) {
      if ((Boolean) stack.getOrDefault(PEDataComponentTypes.ACTIVE, false)) {
        if (!hasEmc(player, stack, 64L, true)) {
          stack.set(PEDataComponentTypes.ACTIVE, false);
        } else {
          WorldHelper.growNearbyRandomly(true, level, player);
          removeEmc(stack, (float) 0.32F);
        }
      } else {
        WorldHelper.growNearbyRandomly(false, level, player);
      }
    }
  }

  @NotNull
  public InteractionResult useOn(UseOnContext ctx) {
    Level level = ctx.getLevel();
    Player player = ctx.getPlayer();
    BlockPos pos = ctx.getClickedPos();
    Direction side = ctx.getClickedFace();
    if (player != null && player.mayUseItemAt(pos, side, ctx.getItemInHand())) {
      if (ctx.isSecondaryUseActive()) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
          if (stack.is(Items.BONE_MEAL)) {
            InteractionResult result = this.useBoneMeal(level, pos, side, player, stack);
            if (result != InteractionResult.PASS) {
              return result;
            }
          }
        }
      } else if (this.plantSeeds(level, player, pos)) {
        return InteractionResult.CONSUME;
      }

      return InteractionResult.FAIL;
    } else {
      return InteractionResult.FAIL;
    }
  }

  private InteractionResult useBoneMeal(
      Level level, BlockPos pos, Direction side, Player player, ItemStack stack) {
    int count = stack.getCount();
    if (count < 4) {
      return InteractionResult.PASS;
    }

    int successfulTargets = 0;

    for (BlockPos currentPos : WorldHelper.horizontalPositionsAround(pos, 15)) {
      boolean wasSuccessful = false;
      BlockState state = level.getBlockState(currentPos);
      BonemealEvent event = EventHooks.fireBonemealEvent(player, level, currentPos, state, stack);
      if (event.isCanceled()) {
        wasSuccessful = event.isSuccessful();
      } else if (event.isValidBonemealTarget()) {
        wasSuccessful = true;
        if (level instanceof ServerLevel serverLevel) {
          BonemealableBlock growable = (BonemealableBlock) state.getBlock();
          if (growable.isBonemealSuccess(level, level.getRandom(), currentPos, state)) {
            growable.performBonemeal(serverLevel, level.getRandom(), currentPos, state);
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            level.levelEvent(1505, currentPos, 0);
          }
        }
      }

      if (wasSuccessful) {
        successfulTargets++;
      } else {
        BlockPos posAgainst = currentPos.relative(side.getOpposite());
        if (level.getBlockState(posAgainst).isFaceSturdy(level, posAgainst, side)
            && BoneMealItem.growWaterPlant(ItemStack.EMPTY, level, currentPos, side)) {
          successfulTargets++;
          if (level.isClientSide()) {
            break;
          }

          player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
          level.levelEvent(1505, currentPos, 0);
        }
      }
    }

    if (!level.isClientSide()) {
      int alreadyRemoved = count - stack.getCount();
      if (alreadyRemoved >= 0 && alreadyRemoved <= 4) {
        stack.shrink(Math.min(4 - alreadyRemoved, successfulTargets));
      }

      if (stack.getCount() != count) {
        player.inventoryMenu.broadcastChanges();
      }
    }

    return (InteractionResult)
        (successfulTargets == 0 ? InteractionResult.FAIL : InteractionResult.CONSUME);
  }

  private boolean plantSeeds(Level level, Player player, BlockPos pos) {
    List<ItemStack> seeds = this.getAllSeeds(player.getInventory().getNonEquipmentItems());
    if (seeds.isEmpty()) {
      return false;
    }

    boolean hasPlantedAny = false;

    for (BlockPos currentPos : WorldHelper.horizontalPositionsAround(pos, 8)) {
      BlockState state = level.getBlockState(currentPos);
      if (!state.isAir()) {
        BlockPos plantPos = currentPos.above();
        BlockPlaceContext placeContext = null;
        Iterator<ItemStack> iterator = seeds.iterator();

        while (iterator.hasNext()) {
          ItemStack stack = iterator.next();
          boolean planted = false;
          if (stack.getItem() instanceof SpecialPlantable plantable) {
            if (plantable.canPlacePlantAtPosition(stack, level, plantPos, Direction.DOWN)) {
              plantable.spawnPlantAtPosition(stack, level, plantPos, Direction.DOWN);
              planted = true;
            }
          } else if (stack.is(PETags.Items.PLANTABLE_SEEDS)
              && stack.getItem() instanceof BlockItem blockItem) {
            if (placeContext == null) {
              placeContext =
                  new BlockPlaceContext(
                      level,
                      player,
                      InteractionHand.MAIN_HAND,
                      stack,
                      new BlockHitResult(
                          Vec3.atCenterOf(currentPos).relative(Direction.UP, 0.5),
                          Direction.UP,
                          currentPos,
                          false));
              ((BlockPlaceContextAccessor) placeContext).projecte$setReplaceClicked(false);
            }

            if (placeContext.canPlace()) {
              BlockState plantState = blockItem.getBlock().getStateForPlacement(placeContext);
              if (plantState != null) {
                level.setBlockAndUpdate(plantPos, plantState);
                level.gameEvent(GameEvent.BLOCK_PLACE, plantPos, Context.of(player, plantState));
                planted = true;
              }
            }
          }

          if (planted) {
            hasPlantedAny = true;
            stack.shrink(1);
            player.inventoryMenu.broadcastChanges();
            if (stack.isEmpty()) {
              iterator.remove();
              if (seeds.isEmpty()) {
                return hasPlantedAny;
              }
            }
            break;
          }
        }
      }
    }

    return hasPlantedAny;
  }

  private List<ItemStack> getAllSeeds(NonNullList<ItemStack> inv) {
    List<ItemStack> result = new ArrayList<>();

    for (ItemStack stack : inv) {
      if (!stack.isEmpty()) {
        Item item = stack.getItem();
        if (item instanceof SpecialPlantable
            || stack.is(PETags.Items.PLANTABLE_SEEDS) && item instanceof BlockItem) {
          result.add(stack);
        }
      }
    }

    return result;
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.harvest.get() != -1) {
      if (pedestal.getActivityCooldown() == 0) {
        WorldHelper.growNearbyRandomly(true, level, pedestal.getEffectBounds(), null);
        pedestal.setActivityCooldown(
            level, pos, ProjectEConfig.server.cooldown.pedestal.harvest.get());
      } else {
        pedestal.decrementActivityCooldown(level, pos);
      }
    }

    return false;
  }

  @NotNull
  @Override
  public List<Component> getPedestalDescription(float tickRate) {
    List<Component> list = new ArrayList<>();
    if (ProjectEConfig.server.cooldown.pedestal.harvest.get() != -1) {
      list.add(PELang.PEDESTAL_HARVEST_GODDESS_1.translateColored(ChatFormatting.BLUE));
      list.add(PELang.PEDESTAL_HARVEST_GODDESS_2.translateColored(ChatFormatting.BLUE));
      list.add(
          PELang.PEDESTAL_HARVEST_GODDESS_3.translateColored(
              ChatFormatting.BLUE,
              MathUtils.tickToSecFormatted(
                  ProjectEConfig.server.cooldown.pedestal.harvest.get(), tickRate)));
    }

    return list;
  }
}
