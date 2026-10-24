package moze_intel.projecte.api.capabilities.item;

import moze_intel.projecte.api.block_entity.IDMPedestal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/** A pedestal item whose effect is limited to a visible world-space area. */
public interface IPedestalRangeItem extends IPedestalItem {
  @NotNull
  default AABB getPedestalEffectBounds(
      @NotNull ItemStack stack, @NotNull BlockPos pos, @NotNull IDMPedestal pedestal) {
    return pedestal.getEffectBounds();
  }
}
