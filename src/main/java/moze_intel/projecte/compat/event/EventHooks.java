package moze_intel.projecte.compat.event;

import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.entity.living.FinalizeSpawnEvent;
import moze_intel.projecte.compat.event.entity.player.BonemealEvent;
import moze_intel.projecte.compat.event.level.ExplosionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class EventHooks {
  private EventHooks() {}

  public static SpawnGroupData finalizeMobSpawn(
      Mob mob,
      ServerLevel level,
      DifficultyInstance difficulty,
      EntitySpawnReason reason,
      SpawnGroupData data) {
    return finalizeMobSpawnWithEvent(mob, level, difficulty, reason, data).spawnData();
  }

  public static EventHooks.MobSpawnResult finalizeMobSpawnWithEvent(
      Mob mob,
      ServerLevel level,
      DifficultyInstance difficulty,
      EntitySpawnReason reason,
      SpawnGroupData data) {
    FinalizeSpawnEvent event =
        NeoForge.EVENT_BUS.post(new FinalizeSpawnEvent(mob, level, difficulty, reason, data));
    SpawnGroupData result =
        event.isCanceled()
            ? null
            : mob.finalizeSpawn(
                level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
    return new EventHooks.MobSpawnResult(result, event.isSpawnCancelled());
  }

  public static boolean onExplosionStart(ServerLevel level, Explosion explosion) {
    return NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(level, explosion)).isCanceled();
  }

  public static BonemealEvent fireBonemealEvent(
      Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
    return NeoForge.EVENT_BUS.post(new BonemealEvent(player, level, pos, state, stack));
  }

  public static void firePlayerSmeltedEvent(Player player, ItemStack stack) {
    player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
  }

  public record MobSpawnResult(SpawnGroupData spawnData, boolean spawnCancelled) {}
}
