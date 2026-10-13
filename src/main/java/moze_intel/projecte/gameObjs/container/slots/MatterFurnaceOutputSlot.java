package moze_intel.projecte.gameObjs.container.slots;

import moze_intel.projecte.compat.event.EventHooks;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MatterFurnaceOutputSlot extends InventoryContainerSlot {
  private final DMFurnaceBlockEntity furnace;
  private final Player player;
  private int removeCount;

  public MatterFurnaceOutputSlot(
      Player player,
      DMFurnaceBlockEntity furnace,
      IItemHandler itemHandler,
      int index,
      int xPosition,
      int yPosition) {
    super(itemHandler, index, xPosition, yPosition);
    this.furnace = furnace;
    this.player = player;
  }

  @Override
  public boolean mayPlace(@NotNull ItemStack stack) {
    return false;
  }

  @NotNull
  @Override
  public ItemStack remove(int amount) {
    if (this.hasItem()) {
      this.removeCount = this.removeCount + Math.min(amount, this.getItem().getCount());
    }

    return super.remove(amount);
  }

  public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
    this.checkTakeAchievements(stack);
    super.onTake(player, stack);
  }

  protected void onQuickCraft(@NotNull ItemStack stack, int pAmount) {
    this.removeCount += pAmount;
    this.checkTakeAchievements(stack);
  }

  protected void checkTakeAchievements(ItemStack stack) {
    stack.onCraftedBy(this.player, this.removeCount);
    if (this.player instanceof ServerPlayer serverPlayer) {
      this.furnace.awardUsedRecipesAndPopExperience(serverPlayer);
    }

    this.removeCount = 0;
    EventHooks.firePlayerSmeltedEvent(this.player, stack);
  }
}
