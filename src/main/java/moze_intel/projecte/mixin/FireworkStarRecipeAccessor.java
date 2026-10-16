package moze_intel.projecte.mixin;

import java.util.Map;
import net.minecraft.world.item.component.FireworkExplosion.Shape;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkStarRecipe.class)
public interface FireworkStarRecipeAccessor {
  @Accessor("shapes")
  Map<Shape, Ingredient> projecte$getShapes();

  @Accessor("trail")
  Ingredient projecte$getTrail();

  @Accessor("twinkle")
  Ingredient projecte$getTwinkle();
}
