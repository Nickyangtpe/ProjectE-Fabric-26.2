package moze_intel.projecte.compat.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class CapabilityHolder {
  private CapabilityHolder() {}

  public interface BlockEntityAccess {
    @Nullable
    default <T, C> T getCapability(BlockCapability<T, C> capability, @Nullable C context) {
      BlockEntity blockEntity = (BlockEntity) this;
      return capability.find(blockEntity.getLevel(), blockEntity.getBlockPos(), context);
    }
  }

  public interface EntityAccess {
    @Nullable
    default <T> T getCapability(EntityCapability<T, Void> capability) {
      return capability.find((Entity) this, null);
    }
  }

  public interface ItemStackAccess {
    @Nullable
    default <T> T getCapability(ItemCapability<T, Void> capability) {
      return capability.find((ItemStack) (Object) this, null);
    }
  }

  public interface LevelAccess {
    @Nullable
    default <T, C> T getCapability(
        BlockCapability<T, C> capability, BlockPos pos, @Nullable C context) {
      return capability.find((Level) this, pos, context);
    }

    @Nullable
    default <T, C> T getCapability(BlockCapability<T, C> capability, BlockPos pos) {
      return capability.find((Level) this, pos, null);
    }
  }
}
