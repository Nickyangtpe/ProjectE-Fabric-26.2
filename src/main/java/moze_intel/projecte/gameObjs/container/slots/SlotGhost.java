package moze_intel.projecte.gameObjs.container.slots;

import java.util.function.Predicate;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.SlotItemHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotGhost extends SlotItemHandler implements ISlotGhost {
  private final Predicate<ItemStack> validator;

  public SlotGhost(
      IItemHandler inv, int slotIndex, int xPos, int yPos, Predicate<ItemStack> validator) {
    super(inv, slotIndex, xPos, yPos);
    this.validator = validator;
  }

  @Override
  public boolean mayPlace(@NotNull ItemStack stack) {
    if (super.mayPlace(stack) && this.validator.test(stack)) {
      this.set(stack);
    }

    return false;
  }

  @Override
  public void initialize(@NotNull ItemStack stack) {
    super.initialize(stack.copyWithCount(1));
  }

  @Override
  public void set(@NotNull ItemStack stack) {
    super.set(stack.copyWithCount(1));
  }

  @Override
  public boolean mayPickup(@NotNull Player player) {
    return false;
  }

  @Override
  public int getMaxStackSize() {
    return 1;
  }

  @Override
  public int getMaxStackSize(@NotNull ItemStack stack) {
    return 1;
  }
}
