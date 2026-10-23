package moze_intel.projecte.compat.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BlockSnapshotCapture {
  private static final ThreadLocal<Deque<BlockSnapshotCapture.Scope>> ACTIVE_CAPTURES =
      ThreadLocal.withInitial(ArrayDeque::new);

  private BlockSnapshotCapture() {}

  public static BlockSnapshotCapture.Scope begin(Level level) {
    BlockSnapshotCapture.Scope scope = new BlockSnapshotCapture.Scope(level);
    ACTIVE_CAPTURES.get().push(scope);
    return scope;
  }

  public static void captureBeforeMutation(Level level, BlockPos pos, int flags) {
    Deque<BlockSnapshotCapture.Scope> scopes = ACTIVE_CAPTURES.get();
    if (scopes.isEmpty()) {
      ACTIVE_CAPTURES.remove();
    } else {
      for (BlockSnapshotCapture.Scope scope : scopes) {
        if (scope.level == level) {
          scope.capture(pos, flags);
        }
      }
    }
  }

  public static boolean deferDrops(Level level, BlockPos pos, List<ItemStack> drops) {
    Deque<BlockSnapshotCapture.Scope> scopes = ACTIVE_CAPTURES.get();
    if (scopes.isEmpty()) {
      ACTIVE_CAPTURES.remove();
      return false;
    }
    for (Scope scope : scopes) {
      if (scope.level == level) {
        BlockSnapshot snapshot = scope.snapshots.get(pos);
        if (snapshot != null) {
          snapshot.deferDrops(drops);
          return true;
        }
      }
    }
    return false;
  }

  public static final class Scope implements AutoCloseable {
    private final Level level;
    private final Map<BlockPos, BlockSnapshot> snapshots = new LinkedHashMap<>();
    private boolean closed;

    private Scope(Level level) {
      this.level = level;
    }

    private void capture(BlockPos pos, int flags) {
      BlockPos immutablePos = pos.immutable();
      this.snapshots.computeIfAbsent(
          immutablePos,
          key -> new BlockSnapshot(this.level, key, this.level.getBlockState(key), flags));
    }

    public List<BlockSnapshot> getChangedSnapshots() {
      List<BlockSnapshot> changed = new ArrayList<>(this.snapshots.size());

      for (BlockSnapshot snapshot : this.snapshots.values()) {
        if (!snapshot.getState().equals(this.level.getBlockState(snapshot.getPos()))) {
          changed.add(snapshot);
        }
      }

      return List.copyOf(changed);
    }

    @Override
    public void close() {
      if (!this.closed) {
        Deque<BlockSnapshotCapture.Scope> scopes = BlockSnapshotCapture.ACTIVE_CAPTURES.get();
        if (scopes.peek() != this) {
          throw new IllegalStateException("Block snapshot captures must close in reverse order");
        }

        scopes.pop();
        this.closed = true;
        if (scopes.isEmpty()) {
          BlockSnapshotCapture.ACTIVE_CAPTURES.remove();
        }
      }
    }
  }
}
