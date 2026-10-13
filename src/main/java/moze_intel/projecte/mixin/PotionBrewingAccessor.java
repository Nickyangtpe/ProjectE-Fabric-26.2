package moze_intel.projecte.mixin;

import java.util.List;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {
  @Accessor("containers")
  List<Ingredient> projecte$getContainers();

  @Accessor("containerMixes")
  List<?> projecte$getContainerMixes();

  @Accessor("potionMixes")
  List<?> projecte$getPotionMixes();
}
