package moze_intel.projecte.api.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface IRelay {
  double getBonusToAdd();

  void addBonus(@NotNull Level var1, @NotNull BlockPos var2);
}
