package moze_intel.projecte.compat.items;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemHandlerCopySlot extends SlotItemHandler {
  public ItemHandlerCopySlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
    super(itemHandler, index, xPosition, yPosition);
  }

  @Override
  public void initialize(@NotNull ItemStack stack) {
    super.initialize(stack.copyWithCount(1));
  }

  @Override
  public void set(@NotNull ItemStack stack) {
    super.set(stack.copyWithCount(1));
  }

  public void setByPlayer(@NotNull ItemStack stack) {
    super.setByPlayer(stack.copyWithCount(1));
  }

  public void setByPlayer(@NotNull ItemStack stack, @Nullable ItemStack oldStack) {
    super.setByPlayer(stack.copyWithCount(1), oldStack);
  }
}
