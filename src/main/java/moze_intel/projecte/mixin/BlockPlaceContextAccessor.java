package moze_intel.projecte.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockPlaceContext.class)
public interface BlockPlaceContextAccessor {
  @Accessor("replaceClicked")
  void projecte$setReplaceClicked(boolean var1);
}
