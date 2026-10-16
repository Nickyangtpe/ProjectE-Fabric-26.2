package moze_intel.projecte.compat.common;

import net.minecraft.world.item.ItemStack;

public interface ItemAbilityProvider {
  boolean canPerformAction(ItemStack var1, ItemAbility var2);
}
