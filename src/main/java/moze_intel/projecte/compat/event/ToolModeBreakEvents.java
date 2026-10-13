package moze_intel.projecte.compat.event;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.tools.PEMorningStar;
import moze_intel.projecte.gameObjs.items.tools.PEPickaxe;
import moze_intel.projecte.utils.ToolHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class ToolModeBreakEvents {
  private static final Map<ServerPlayer, Map<BlockPos, ToolModeBreakEvents.CapturedDig>> CAPTURED =
      new WeakHashMap<>();
  private static final Map<ServerPlayer, ToolModeBreakEvents.PendingDig> PENDING =
      new WeakHashMap<>();
  private static final ThreadLocal<Boolean> PROCESSING_MODE_DIG =
      ThreadLocal.withInitial(() -> false);

  private ToolModeBreakEvents() {}

  public static void register() {
    PlayerBlockBreakEvents.BEFORE.register(ToolModeBreakEvents::beforeBlockBreak);
    PlayerBlockBreakEvents.AFTER.register(ToolModeBreakEvents::afterBlockBreak);
    PlayerBlockBreakEvents.CANCELED.register(ToolModeBreakEvents::cancelledBlockBreak);
  }

  public static void captureDigAction(
      ServerPlayer player, BlockPos pos, Action action, Direction hitSide) {
    if (action == Action.ABORT_DESTROY_BLOCK) {
      removeCapturedDig(player, pos);
    } else if (action == Action.START_DESTROY_BLOCK || action == Action.STOP_DESTROY_BLOCK) {
      BlockPos immutablePos = pos.immutable();
      ItemStack stack = player.getMainHandItem();
      PEPickaxe.PickaxeMode mode = getPickaxeMode(stack);
      if (mode != null
          && mode != PEPickaxe.PickaxeMode.STANDARD
          && !ProjectEConfig.server.items.disableAllRadiusMining.get()) {
        ToolModeBreakEvents.CapturedDig previous = getCapturedDig(player, immutablePos);
        if (action != Action.STOP_DESTROY_BLOCK || previous == null) {
          CAPTURED
              .computeIfAbsent(player, ignored -> new HashMap<>())
              .put(
                  immutablePos,
                  new ToolModeBreakEvents.CapturedDig(immutablePos, hitSide, stack.copy(), mode));
        }
      } else {
        removeCapturedDig(player, immutablePos);
      }
    }
  }

  private static boolean beforeBlockBreak(
      Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
    if (!PROCESSING_MODE_DIG.get()
        && !level.isClientSide()
        && player instanceof ServerPlayer serverPlayer) {
      ToolModeBreakEvents.CapturedDig captured = getCapturedDig(serverPlayer, pos);
      ItemStack stack = captured == null ? player.getMainHandItem() : captured.stack();
      PEPickaxe.PickaxeMode mode = captured == null ? getPickaxeMode(stack) : captured.mode();
      if (mode != null
          && mode != PEPickaxe.PickaxeMode.STANDARD
          && !ProjectEConfig.server.items.disableAllRadiusMining.get()) {
        Direction hitSide;
        if (captured != null) {
          hitSide = captured.hitSide();
        } else {
          BlockHitResult hit = PEPickaxe.getPlayerPOVHitResultForMode(level, player);
          if (hit.getType() == Type.MISS || !pos.equals(hit.getBlockPos())) {
            PENDING.remove(serverPlayer);
            return true;
          }

          hitSide = hit.getDirection();
        }

        PENDING.put(
            serverPlayer,
            new ToolModeBreakEvents.PendingDig(
                pos.immutable(), state, hitSide, stack.copy(), mode));
        return true;
      } else {
        PENDING.remove(serverPlayer);
        if (captured != null) {
          removeCapturedDig(serverPlayer, pos);
        }

        return true;
      }
    } else {
      return true;
    }
  }

  private static void afterBlockBreak(
      Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
    if (!PROCESSING_MODE_DIG.get()
        && !level.isClientSide()
        && player instanceof ServerPlayer serverPlayer) {
      removeCapturedDig(serverPlayer, pos);
      ToolModeBreakEvents.PendingDig pending = PENDING.remove(serverPlayer);
      if (pending != null && pending.pos().equals(pos) && pending.state().equals(state)) {
        try {
          PROCESSING_MODE_DIG.set(true);
          ToolHelper.digBasedOnModeAfterPrimaryBreak(
              pending.stack(), level, pos, serverPlayer, pending.hitSide(), pending.mode());
        } finally {
          PROCESSING_MODE_DIG.remove();
        }
      }
    }
  }

  private static void cancelledBlockBreak(
      Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
    if (player instanceof ServerPlayer serverPlayer) {
      ToolModeBreakEvents.PendingDig pending = PENDING.get(serverPlayer);
      if (pending != null && pending.pos().equals(pos)) {
        PENDING.remove(serverPlayer);
      }

      removeCapturedDig(serverPlayer, pos);
    }
  }

  private static ToolModeBreakEvents.CapturedDig getCapturedDig(ServerPlayer player, BlockPos pos) {
    Map<BlockPos, ToolModeBreakEvents.CapturedDig> captures = CAPTURED.get(player);
    return captures == null ? null : captures.get(pos);
  }

  private static void removeCapturedDig(ServerPlayer player, BlockPos pos) {
    Map<BlockPos, ToolModeBreakEvents.CapturedDig> captures = CAPTURED.get(player);
    if (captures != null) {
      captures.remove(pos);
      if (captures.isEmpty()) {
        CAPTURED.remove(player);
      }
    }
  }

  private static PEPickaxe.PickaxeMode getPickaxeMode(ItemStack stack) {
    if (stack.getItem() instanceof PEPickaxe pickaxe) {
      return pickaxe.getMode(stack);
    } else {
      return stack.getItem() instanceof PEMorningStar morningStar
          ? morningStar.getMode(stack)
          : null;
    }
  }

  private record CapturedDig(
      BlockPos pos, Direction hitSide, ItemStack stack, PEPickaxe.PickaxeMode mode) {}

  private record PendingDig(
      BlockPos pos,
      BlockState state,
      Direction hitSide,
      ItemStack stack,
      PEPickaxe.PickaxeMode mode) {}
}
