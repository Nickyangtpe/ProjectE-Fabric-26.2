package moze_intel.projecte.api.world_transmutation;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface IWorldTransmutationFunction {
  @Nullable
  BlockState result(@NotNull BlockState var1, boolean var2);
}
