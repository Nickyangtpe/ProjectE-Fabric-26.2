package moze_intel.projecte.api.world_transmutation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface IWorldTransmutation extends IWorldTransmutationFunction
    permits SimpleWorldTransmutation, WorldTransmutation {
  Holder<Block> origin();

  boolean hasAlternate();

  @Nullable
  @Override
  BlockState result(@NotNull BlockState var1, boolean var2);

  boolean canTransmute(@NotNull BlockState var1);
}
