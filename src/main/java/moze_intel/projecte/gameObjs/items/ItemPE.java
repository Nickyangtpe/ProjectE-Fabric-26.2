package moze_intel.projecte.gameObjs.items;

import java.util.Objects;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.ItemAbilityProvider;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import org.jetbrains.annotations.Range;

public class ItemPE extends Item implements ItemAbilityProvider {
  public ItemPE(Properties props) {
    super(props);
  }

  @Override
  public boolean canPerformAction(ItemStack stack, ItemAbility action) {
    return false;
  }

  public boolean shouldCauseReequipAnimation(
      ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
    if (oldStack.getItem() != newStack.getItem()) {
      return true;
    } else {
      return oldStack.getOrDefault(PEDataComponentTypes.ACTIVE, false)
              != newStack.getOrDefault(PEDataComponentTypes.ACTIVE, false)
          ? true
          : this instanceof IModeChanger<?> modeChanger
              && !modeMatches(modeChanger, oldStack, newStack);
    }
  }

  private static <MODE> boolean modeMatches(
      IModeChanger<MODE> modeChanger, ItemStack oldStack, ItemStack newStack) {
    return Objects.equals(modeChanger.getMode(oldStack), modeChanger.getMode(newStack));
  }

  private static @Range(from = 0L, to = Long.MAX_VALUE) long getEmc(ItemStack stack) {
    return (Long) stack.getOrDefault(PEDataComponentTypes.STORED_EMC, 0L);
  }

  public static void removeEmc(ItemStack stack, float amount) {
    removeEmc(stack, EMCHelper.removeFractionalEMC(stack, amount));
  }

  public static void removeEmc(
      ItemStack stack, @Range(from = 0L, to = Long.MAX_VALUE) long amount) {
    if (amount > 0L) {
      stack.update(
          PEDataComponentTypes.STORED_EMC, 0L, amount, (emc, change) -> Math.max(emc - change, 0L));
    }
  }

  public static boolean hasEmc(
      Player player,
      ItemStack stack,
      @Range(from = 0L, to = Long.MAX_VALUE) long amount,
      boolean subUnitRemoval) {
    return subUnitRemoval && getEmc(stack) > 0L ? true : consumeFuel(player, stack, amount, false);
  }

  public static boolean consumeFuel(
      Player player,
      ItemStack stack,
      @Range(from = 0L, to = Long.MAX_VALUE) long amount,
      boolean shouldRemove) {
    if (amount <= 0L) {
      return true;
    }

    long current = getEmc(stack);
    boolean updateEmc = shouldRemove;
    if (current < amount) {
      long consume = EMCHelper.consumePlayerFuel(player, amount - current);
      if (consume == -1L) {
        return false;
      }

      long spaceFor = Long.MAX_VALUE - current;
      if (consume > spaceFor) {
        if (shouldRemove) {
          consume -= amount;
          shouldRemove = false;
          if (consume > spaceFor) {
            current = Long.MAX_VALUE;
          } else {
            current += consume;
          }
        } else {
          current = Long.MAX_VALUE;
        }
      } else {
        current += consume;
      }

      updateEmc = true;
    }

    if (shouldRemove) {
      current -= amount;
    }

    if (updateEmc) {
      stack.set(PEDataComponentTypes.STORED_EMC, current);
    }

    return true;
  }

  public static boolean hotBarOrOffHand(int slot) {
    return slot < Inventory.getSelectionSize() || slot == 40;
  }

  public static boolean hotBarOrOffHand(ItemStack stack, Entity entity, EquipmentSlot slot) {
    return !(entity instanceof Player player)
        ? false
        : slot == EquipmentSlot.MAINHAND
            || slot == EquipmentSlot.OFFHAND
            || player.getMainHandItem() == stack
            || player.getOffhandItem() == stack;
  }
}
