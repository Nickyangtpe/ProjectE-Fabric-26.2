package moze_intel.projecte.gameObjs.customRecipes;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class WrappedShapelessRecipe implements CraftingRecipe {
  private final ShapelessRecipe internal;

  protected WrappedShapelessRecipe(ShapelessRecipe internal) {
    this.internal = internal;
  }

  public ShapelessRecipe getInternal() {
    return this.internal;
  }

  public abstract ItemStack assemble(@NotNull CraftingInput var1);

  public boolean matches(@NotNull CraftingInput inv, @NotNull Level world) {
    return this.internal.matches(inv, world) && !this.assemble(inv).isEmpty();
  }

  public CraftingBookCategory category() {
    return this.internal.category();
  }

  public String group() {
    return this.internal.group();
  }

  public boolean showNotification() {
    return this.internal.showNotification();
  }

  public PlacementInfo placementInfo() {
    return this.internal.placementInfo();
  }

  public List<Ingredient> getIngredients() {
    return this.internal.placementInfo().ingredients();
  }
}
