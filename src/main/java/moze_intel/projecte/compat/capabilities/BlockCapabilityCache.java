package moze_intel.projecte.compat.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class BlockCapabilityCache<T, C> {
  private final BlockCapability<T, C> capability;
  private final Level level;
  private final BlockPos pos;
  private final C context;

  private BlockCapabilityCache(
      BlockCapability<T, C> capability, Level level, BlockPos pos, C context) {
    this.capability = capability;
    this.level = level;
    this.pos = pos;
    this.context = context;
  }

  public static <T, C> BlockCapabilityCache<T, C> create(
      BlockCapability<T, C> capability, Level level, BlockPos pos, C context) {
    return new BlockCapabilityCache<>(capability, level, pos, context);
  }

  @Nullable
  public T getCapability() {
    return this.capability.find(this.level, this.pos, this.context);
  }

  public boolean isValid() {
    return this.getCapability() != null;
  }
}
