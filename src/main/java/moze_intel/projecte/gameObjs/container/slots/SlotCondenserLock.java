package moze_intel.projecte.gameObjs.container.slots;

import java.util.function.Supplier;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotCondenserLock extends Slot {
  private static final Container emptyInventory = new SimpleContainer(0);
  private final Supplier<ItemInfo> lockInfo;

  public SlotCondenserLock(Supplier<ItemInfo> lockInfo, int index, int xPosition, int yPosition) {
    super(emptyInventory, index, xPosition, yPosition);
    this.lockInfo = lockInfo;
  }

  public boolean mayPlace(@NotNull ItemStack stack) {
    if (!stack.isEmpty() && SlotPredicates.HAS_EMC.test(stack)) {
      this.set(stack.copyWithCount(1));
    }

    return false;
  }

  public boolean mayPickup(@NotNull Player player) {
    return false;
  }

  public int getMaxStackSize() {
    return 1;
  }

  @NotNull
  public ItemStack getItem() {
    ItemInfo lockInfo = this.lockInfo.get();
    return lockInfo == null ? ItemStack.EMPTY : lockInfo.createStack();
  }

  public void set(@NotNull ItemStack stack) {}

  public void onQuickCraft(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {}

  @NotNull
  public ItemStack remove(int amount) {
    return this.getItem();
  }
}
