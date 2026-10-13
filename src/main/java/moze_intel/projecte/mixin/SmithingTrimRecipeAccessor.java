package moze_intel.projecte.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTrimRecipe.class)
public interface SmithingTrimRecipeAccessor {
  @Accessor("pattern")
  Holder<TrimPattern> projecte$getPattern();
}
