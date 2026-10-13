package moze_intel.projecte.mixin;

import net.minecraft.world.item.crafting.FireworkRocketRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketRecipe.class)
public interface FireworkRocketRecipeAccessor {
  @Accessor("fuel")
  Ingredient projecte$getFuel();
}
