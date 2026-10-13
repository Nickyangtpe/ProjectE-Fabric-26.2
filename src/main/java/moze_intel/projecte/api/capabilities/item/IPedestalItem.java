package moze_intel.projecte.api.capabilities.item;

import java.util.List;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public interface IPedestalItem {
  <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack var1, @NotNull Level var2, @NotNull BlockPos var3, @NotNull PEDESTAL var4);

  @NotNull
  List<Component> getPedestalDescription(float var1);
}
