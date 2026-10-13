package moze_intel.projecte.api.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public interface IDMPedestal {
  int getActivityCooldown();

  void setActivityCooldown(@NotNull Level var1, @NotNull BlockPos var2, int var3);

  void decrementActivityCooldown(@NotNull Level var1, @NotNull BlockPos var2);

  AABB getEffectBounds();
}
