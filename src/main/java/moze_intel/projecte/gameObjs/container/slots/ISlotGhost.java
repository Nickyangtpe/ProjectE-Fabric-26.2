package moze_intel.projecte.gameObjs.container.slots;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface ISlotGhost {
  private Slot self() {
    return (Slot) this;
  }

  default boolean tryClear() {
    if (!this.self().getItem().isEmpty()) {
      this.self().set(ItemStack.EMPTY);
      return true;
    } else {
      return false;
    }
  }
}
