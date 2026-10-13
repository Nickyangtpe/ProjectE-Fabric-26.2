package moze_intel.projecte.emc.mappers.recipe.special;

import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.Composite;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.WithRemainder;

public final class RecipeDisplayResultMatcher {
  private RecipeDisplayResultMatcher() {}

  public static boolean hasMatchingResult(
      RecipeHolder<?> recipeHolder, Predicate<ItemStack> matcher) {
    for (RecipeDisplay display : recipeHolder.value().display()) {
      if (matches(display.result(), matcher)) {
        return true;
      }
    }

    return false;
  }

  private static boolean matches(SlotDisplay display, Predicate<ItemStack> matcher) {
    if (display instanceof ItemStackSlotDisplay itemStackDisplay) {
      return matcher.test(itemStackDisplay.stack().create());
    } else if (display instanceof ItemSlotDisplay itemDisplay) {
      return matcher.test(((Item) itemDisplay.item().value()).getDefaultInstance());
    } else if (display instanceof Composite composite) {
      return composite.contents().stream().anyMatch(content -> matches(content, matcher));
    } else {
      return display instanceof WithRemainder withRemainder
          ? matches(withRemainder.input(), matcher)
          : false;
    }
  }
}
