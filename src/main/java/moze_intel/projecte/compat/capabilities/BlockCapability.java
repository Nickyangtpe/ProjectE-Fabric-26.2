package moze_intel.projecte.compat.capabilities;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockCapability<T, C> {
  private final BlockApiLookup<T, C> lookup;

  private BlockCapability(BlockApiLookup<T, C> lookup) {
    this.lookup = lookup;
  }

  public static <T, C> BlockCapability<T, C> createSided(Identifier id, Class<T> apiClass) {
    Class<C> contextClass = (Class<C>) Direction.class;
    return new BlockCapability<>(BlockApiLookup.get(id, apiClass, contextClass));
  }

  public static <T, C> BlockCapability<T, C> create(Identifier id, Class<T> apiClass) {
    Class<C> contextClass = (Class<C>) Void.class;
    return new BlockCapability<>(BlockApiLookup.get(id, apiClass, contextClass));
  }

  @NotNull
  public BlockApiLookup<T, C> lookup() {
    return this.lookup;
  }

  @Nullable
  public T find(@NotNull Level level, @NotNull BlockPos pos, @Nullable C context) {
    return (T)
        (level instanceof ServerLevel serverLevel
            ? this.lookup.find(serverLevel, pos, context)
            : this.lookup.find(level, pos, context));
  }

  public <BE extends BlockEntity> void registerForBlockEntity(
      ICapabilityProvider<BE, C, T> provider, BlockEntityType<BE> type) {
    this.lookup.registerForBlockEntity(
        (blockEntity, context) -> provider.getCapability((BE) blockEntity, (C) context), type);
  }
}
