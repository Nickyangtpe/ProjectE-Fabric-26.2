package moze_intel.projecte.gameObjs.container.slots;

import java.util.function.Predicate;
import moze_intel.projecte.compat.items.IItemHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ValidatedSlot extends InventoryContainerSlot {
  private final Predicate<ItemStack> validator;

  public ValidatedSlot(
      IItemHandler itemHandler,
      int index,
      int xPosition,
      int yPosition,
      Predicate<ItemStack> validator) {
    super(itemHandler, index, xPosition, yPosition);
    this.validator = validator;
  }

  @Override
  public boolean mayPlace(@NotNull ItemStack stack) {
    return super.mayPlace(stack) && this.validator.test(stack);
  }
}
