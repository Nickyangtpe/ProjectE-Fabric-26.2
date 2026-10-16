package moze_intel.projecte.compat.event.entity.living;

import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.ICancellableEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import org.jetbrains.annotations.Nullable;

public final class FinalizeSpawnEvent extends Event implements ICancellableEvent {
  private final Mob entity;
  private final ServerLevel level;
  private final EntitySpawnReason spawnType;
  private DifficultyInstance difficulty;
  @Nullable private SpawnGroupData spawnData;
  private boolean spawnCancelled;

  public FinalizeSpawnEvent(
      Mob entity,
      ServerLevel level,
      DifficultyInstance difficulty,
      EntitySpawnReason spawnType,
      @Nullable SpawnGroupData spawnData) {
    this.entity = entity;
    this.level = level;
    this.difficulty = difficulty;
    this.spawnType = spawnType;
    this.spawnData = spawnData;
  }

  public Mob getEntity() {
    return this.entity;
  }

  public ServerLevel getLevel() {
    return this.level;
  }

  public EntitySpawnReason getSpawnType() {
    return this.spawnType;
  }

  public DifficultyInstance getDifficulty() {
    return this.difficulty;
  }

  public void setDifficulty(DifficultyInstance difficulty) {
    this.difficulty = difficulty;
  }

  @Nullable
  public SpawnGroupData getSpawnData() {
    return this.spawnData;
  }

  public void setSpawnData(@Nullable SpawnGroupData spawnData) {
    this.spawnData = spawnData;
  }

  @Override
  public void setCanceled(boolean canceled) {
    super.setCanceled(canceled);
  }

  public void setSpawnCancelled(boolean spawnCancelled) {
    this.spawnCancelled = spawnCancelled;
  }

  public boolean isSpawnCancelled() {
    return this.spawnCancelled;
  }
}
