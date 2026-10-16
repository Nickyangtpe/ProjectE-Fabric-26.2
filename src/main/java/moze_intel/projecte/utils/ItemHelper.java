package moze_intel.projecte.utils;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemHelper {
  public static InteractionResult actionResultFromType(InteractionResult type, ItemStack stack) {
    return (InteractionResult)
        (type != InteractionResult.SUCCESS && type != InteractionResult.SUCCESS_SERVER
            ? type
            : InteractionResult.SUCCESS.heldItemTransformedTo(stack));
  }

  public static boolean compactInventory(IItemHandlerModifiable inventory) {
    List<ItemStack> temp = new ArrayList<>();
    int i = 0;

    for (int slots = inventory.getSlots(); i < slots; i++) {
      ItemStack stackInSlot = inventory.getStackInSlot(i);
      if (!stackInSlot.isEmpty()) {
        temp.add(stackInSlot);
        inventory.setStackInSlot(i, ItemStack.EMPTY);
      }
    }

    for (ItemStack s : temp) {
      ItemHandlerHelper.insertItemStacked(inventory, s, false);
    }

    return temp.isEmpty();
  }

  public static IItemHandlerModifiable immutableCopy(IItemHandler toCopy) {
    int slots = toCopy.getSlots();
    final List<ItemStack> list = new ArrayList<>(slots);

    for (int i = 0; i < slots; i++) {
      list.add(toCopy.getStackInSlot(i).copy());
    }

    return new IItemHandlerModifiable() {
      @Override
      public void setStackInSlot(int slot, @NotNull ItemStack stack) {}

      @Override
      public int getSlots() {
        return list.size();
      }

      @NotNull
      @Override
      public ItemStack getStackInSlot(int slot) {
        return list.get(slot);
      }

      @NotNull
      @Override
      public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
      }

      @NotNull
      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
      }

      @Override
      public int getSlotLimit(int slot) {
        return this.getStackInSlot(slot).getMaxStackSize();
      }

      @Override
      public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
      }
    };
  }

  public static boolean isRepairableDamagedItem(ItemStack stack) {
    return stack.isDamageableItem()
        && stack.has(DataComponents.REPAIRABLE)
        && stack.getDamageValue() > 0;
  }

  public static int simulateFit(NonNullList<ItemStack> inv, ItemStack stack) {
    int remainder = stack.getCount();

    for (ItemStack invStack : inv) {
      if (invStack.isEmpty()) {
        return 0;
      }

      if (ItemStack.isSameItemSameComponents(stack, invStack)) {
        int amountSlotNeeds = invStack.getMaxStackSize() - invStack.getCount();
        if (amountSlotNeeds > 0) {
          if (remainder <= amountSlotNeeds) {
            return 0;
          }

          remainder -= amountSlotNeeds;
        }
      }
    }

    return remainder;
  }

  public static ItemStack size(ItemStack stack, int size) {
    return size > 0 && !stack.isEmpty() ? stack.copyWithCount(size) : ItemStack.EMPTY;
  }

  public static BlockState stackToState(ItemStack stack, @Nullable BlockPlaceContext context) {
    if (stack.getItem() instanceof BlockItem blockItem) {
      return context == null
          ? blockItem.getBlock().defaultBlockState()
          : blockItem.getBlock().getStateForPlacement(context);
    } else {
      return null;
    }
  }
}
