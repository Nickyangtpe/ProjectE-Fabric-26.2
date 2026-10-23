package moze_intel.projecte.utils;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntSupplier;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.level.BlockEvent;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.util.BlockSnapshot;
import moze_intel.projecte.compat.util.BlockSnapshotCapture;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.integration.IntegrationHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.Before;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class PlayerHelper {
  public static final ObjectiveCriteria SCOREBOARD_EMC =
      new ReadOnlyScoreCriteria("projecte:emc_score");

  public static boolean checkedPlaceBlock(
      Player player, Level level, BlockPos pos, BlockState state) {
    return hasEditPermission(player, level, pos)
        && partiallyCheckedPlaceBlock(player, level, pos, state);
  }

  private static boolean partiallyCheckedPlaceBlock(
      Player player, Level level, BlockPos pos, BlockState state) {
    SignBlockEntity oldSign = null;
    if (state.getBlock() instanceof SignBlock
        && level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
      oldSign = sign;
    }

    List<BlockSnapshot> blockSnapshots;
    try (BlockSnapshotCapture.Scope capture = BlockSnapshotCapture.begin(level)) {
      if (!level.setBlock(pos, state, 3)) {
        return false;
      }

      blockSnapshots = capture.getChangedSnapshots();
    }

    if (blockSnapshots.isEmpty()) {
      return false;
    }

    boolean eventResult = false;
    if (blockSnapshots.size() > 1) {
      eventResult =
          NeoForge.EVENT_BUS
              .post(
                  new BlockEvent.EntityMultiPlaceEvent(
                      blockSnapshots, Blocks.AIR.defaultBlockState(), player))
              .isCanceled();
    } else if (blockSnapshots.size() == 1) {
      eventResult =
          NeoForge.EVENT_BUS
              .post(
                  new BlockEvent.EntityPlaceEvent(
                      blockSnapshots.getFirst(), Blocks.AIR.defaultBlockState(), player))
              .isCanceled();
    }

    if (eventResult) {
      for (BlockSnapshot revertedSnapshot : blockSnapshots.reversed()) {
        revertedSnapshot.restore(revertedSnapshot.getFlags() | 2);
      }
    } else {
      for (BlockSnapshot snap : blockSnapshots) {
        snap.dropContents();
        BlockState previousState = snap.getState();
        BlockPos snapPos = snap.getPos();
        BlockState newBlock = level.getBlockState(snapPos);
        newBlock.onPlace(level, snapPos, previousState, false);
        if (oldSign != null && snapPos.equals(pos) && newBlock.hasBlockEntity()) {
          WorldHelper.copySignData(level, pos, oldSign);
        }
      }
    }

    return !eventResult;
  }

  public static boolean checkedReplaceBlock(
      ServerPlayer player, Level level, BlockPos pos, BlockState state) {
    return hasBreakPermission(player, level, pos)
        && partiallyCheckedPlaceBlock(player, level, pos, state);
  }

  public static ItemStack findFirstItem(Player player, Holder<Item> consumeFrom) {
    for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
      if (!s.isEmpty() && s.is(consumeFrom)) {
        return s;
      }
    }

    return ItemStack.EMPTY;
  }

  public static boolean checkHotbar(Player player, BiPredicate<Player, ItemStack> checker) {
    for (int i = 0; i < Inventory.getSelectionSize(); i++) {
      ItemStack stack = player.getInventory().getItem(i);
      if (!stack.isEmpty() && checker.test(player, stack)) {
        return true;
      }
    }

    return false;
  }

  public static boolean checkHotbarCurios(Player player, BiPredicate<Player, ItemStack> checker) {
    if (checkHotbar(player, checker)) {
      return true;
    }

    ItemStack offhand = player.getOffhandItem();
    if (!offhand.isEmpty() && checker.test(player, offhand)) {
      return true;
    }

    IItemHandler curios = IntegrationHelper.getAccessoryInventory(player);
    if (curios != null) {
      int i = 0;

      for (int slots = curios.getSlots(); i < slots; i++) {
        ItemStack stack = curios.getStackInSlot(i);
        if (!stack.isEmpty() && checker.test(player, stack)) {
          return true;
        }
      }
    }

    return false;
  }

  public static boolean checkMainInventory(Player player, BiPredicate<Player, ItemStack> checker) {
    List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();

    for (int slot = Inventory.getSelectionSize(); slot < inventory.size(); slot++) {
      ItemStack stack = inventory.get(slot);
      if (!stack.isEmpty() && checker.test(player, stack)) {
        return true;
      }
    }

    return false;
  }

  public static BlockHitResult getBlockLookingAt(Player player, double maxDistance) {
    return (BlockHitResult) player.pick(maxDistance, 1.0F, false);
  }

  public static Vec3 getLookTarget(Player player, double maxDistance) {
    Vec3 lookAngle = player.getLookAngle();
    return player
        .getEyePosition()
        .add(lookAngle.x * maxDistance, lookAngle.y * maxDistance, lookAngle.z * maxDistance);
  }

  public static boolean hasBreakPermission(ServerPlayer player, Level level, BlockPos pos) {
    return hasEditPermission(player, level, pos) && checkBreakPermission(player, level, pos);
  }

  public static boolean checkBreakPermission(ServerPlayer player, Level level, BlockPos pos) {
    return ((Before) PlayerBlockBreakEvents.BEFORE.invoker())
        .beforeBlockBreak(level, player, pos, level.getBlockState(pos), level.getBlockEntity(pos));
  }

  public static boolean hasEditPermission(Player player, Level level, BlockPos pos) {
    if (level instanceof ServerLevel serverLevel && !player.mayInteract(serverLevel, pos)) {
      return false;
    } else {
      for (Direction e : Constants.DIRECTIONS) {
        if (!player.mayUseItemAt(pos, e, ItemStack.EMPTY)) {
          return false;
        }
      }

      return true;
    }
  }

  public static void resetCooldown(Player player) {
    player.resetAttackStrengthTicker();
    PECore.packetHandler().resetCooldown((ServerPlayer) player);
  }

  public static void swingItem(Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer serverPlayer
        && player.level() instanceof ServerLevel level) {
      int action = hand == InteractionHand.MAIN_HAND ? 0 : 3;
      level
          .getChunkSource()
          .sendToTrackingPlayersAndSelf(serverPlayer, new ClientboundAnimatePacket(player, action));
    }
  }

  public static void updateScore(
      ServerPlayer player, ObjectiveCriteria objective, BigInteger value) {
    updateScore(player, objective, MathUtils.clampToInt(value));
  }

  public static void updateScore(ServerPlayer player, ObjectiveCriteria objective, int value) {
    player.level().getScoreboard().forAllObjectives(objective, player, score -> score.set(value));
  }

  public static boolean checkFeedCooldown(Player player) {
    return player.getFoodData().needsFood()
        && checkCooldown(
            player, PEItems.BODY_STONE.get(), ProjectEConfig.server.cooldown.player.feed);
  }

  public static boolean checkHealCooldown(Player player) {
    return player.getHealth() < player.getMaxHealth()
        && checkCooldown(
            player, PEItems.SOUL_STONE.get(), ProjectEConfig.server.cooldown.player.heal);
  }

  public static boolean checkCooldown(Player player, Item item, IntSupplier cooldownSupplier) {
    ItemCooldowns cooldowns = player.getCooldowns();
    ItemStack cooldownStack = new ItemStack(item);
    if (cooldowns.isOnCooldown(cooldownStack)) {
      return false;
    }

    int cooldown = cooldownSupplier.getAsInt();
    if (cooldown == -1) {
      return false;
    }

    if (cooldown > 0) {
      cooldowns.addCooldown(cooldownStack, cooldown);
    }

    return true;
  }
}
