package moze_intel.projecte.gameObjs.container.slots.transmutation;

import java.math.BigInteger;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.InventoryContainerSlot;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotOutput extends InventoryContainerSlot {
  private final TransmutationInventory inv;

  public SlotOutput(TransmutationInventory inv, int index, int x, int y) {
    super(inv, index, x, y);
    this.inv = inv;
  }

  protected void onSwapCraft(int amount) {
    this.remove(amount);
  }

  @NotNull
  @Override
  public ItemStack remove(int amount) {
    if (amount <= 0) {
      return ItemStack.EMPTY;
    }

    ItemStack stack = ItemHelper.size(this.getItem(), amount);
    long emcValue = IEMCProxy.INSTANCE.getValue(stack);
    // A displayed target can become valueless after an EMC remap; it is never free output.
    if (emcValue <= 0L) {
      return ItemStack.EMPTY;
    }
    BigInteger bigEmcValue = BigInteger.valueOf(emcValue);
    if (amount > 1) {
      bigEmcValue = bigEmcValue.multiply(BigInteger.valueOf(amount));
      if (bigEmcValue.compareTo(this.inv.getAvailableEmc()) > 0) {
        return ItemStack.EMPTY;
      }
    } else if (emcValue > this.inv.getAvailableEmcAsLong()) {
      return ItemStack.EMPTY;
    }

    if (this.inv.isServer()) {
      this.inv.removeEmc(bigEmcValue);
    }

    return stack;
  }

  @Override
  public void initialize(@NotNull ItemStack stack) {}

  @Override
  public void set(@NotNull ItemStack stack) {}

  @Override
  public boolean mayPlace(@NotNull ItemStack stack) {
    return false;
  }

  @Override
  public boolean mayPickup(@NotNull Player player) {
    long emcValue = IEMCProxy.INSTANCE.getValue(this.getItem());
    return this.hasItem() && emcValue > 0L && emcValue <= this.inv.getAvailableEmcAsLong();
  }
}
