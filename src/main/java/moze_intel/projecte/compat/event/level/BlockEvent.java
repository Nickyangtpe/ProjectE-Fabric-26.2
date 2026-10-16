package moze_intel.projecte.compat.event.level;

import java.util.List;
import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.ICancellableEvent;
import moze_intel.projecte.compat.util.BlockSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEvent extends Event {
  private final Level level;
  private final BlockPos pos;
  private final BlockState state;

  protected BlockEvent(Level level, BlockPos pos, BlockState state) {
    this.level = level;
    this.pos = pos.immutable();
    this.state = state;
  }

  public Level getLevel() {
    return this.level;
  }

  public BlockPos getPos() {
    return this.pos;
  }

  public BlockState getState() {
    return this.state;
  }

  public static class EntityMultiPlaceEvent extends BlockEvent implements ICancellableEvent {
    private final List<BlockSnapshot> blockSnapshots;
    private final BlockState placedAgainst;
    private final Entity entity;

    public EntityMultiPlaceEvent(
        List<BlockSnapshot> blockSnapshots, BlockState placedAgainst, Entity entity) {
      super(
          requireFirst(blockSnapshots).getLevel(),
          requireFirst(blockSnapshots).getPos(),
          requireFirst(blockSnapshots).getState());
      this.blockSnapshots = List.copyOf(blockSnapshots);
      this.placedAgainst = placedAgainst;
      this.entity = entity;
    }

    private static BlockSnapshot requireFirst(List<BlockSnapshot> blockSnapshots) {
      if (blockSnapshots != null && !blockSnapshots.isEmpty()) {
        return blockSnapshots.getFirst();
      } else {
        throw new IllegalArgumentException(
            "A multi-place event requires at least one block snapshot");
      }
    }

    public List<BlockSnapshot> getBlockSnapshots() {
      return this.blockSnapshots;
    }

    public BlockState getPlacedAgainst() {
      return this.placedAgainst;
    }

    public Entity getEntity() {
      return this.entity;
    }
  }

  public static class EntityPlaceEvent extends BlockEvent implements ICancellableEvent {
    private final BlockSnapshot blockSnapshot;
    private final BlockState placedAgainst;
    private final Entity entity;

    public EntityPlaceEvent(BlockSnapshot blockSnapshot, BlockState placedAgainst, Entity entity) {
      super(blockSnapshot.getLevel(), blockSnapshot.getPos(), blockSnapshot.getState());
      this.blockSnapshot = blockSnapshot;
      this.placedAgainst = placedAgainst;
      this.entity = entity;
    }

    public BlockSnapshot getBlockSnapshot() {
      return this.blockSnapshot;
    }

    public BlockState getPlacedAgainst() {
      return this.placedAgainst;
    }

    public Entity getEntity() {
      return this.entity;
    }
  }

  public static class FluidPlaceBlockEvent extends BlockEvent implements ICancellableEvent {
    private final BlockPos liquidPos;
    private BlockState newState;

    public FluidPlaceBlockEvent(
        Level level, BlockPos pos, BlockPos liquidPos, BlockState newState) {
      super(level, pos, newState);
      this.liquidPos = liquidPos.immutable();
      this.newState = newState;
    }

    public BlockPos getLiquidPos() {
      return this.liquidPos;
    }

    public BlockState getNewState() {
      return this.newState;
    }

    public void setNewState(BlockState newState) {
      this.newState = newState;
    }
  }
}
