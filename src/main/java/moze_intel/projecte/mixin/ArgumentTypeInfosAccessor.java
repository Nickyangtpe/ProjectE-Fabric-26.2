package moze_intel.projecte.mixin;

import java.util.Map;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ArgumentTypeInfos.class)
public interface ArgumentTypeInfosAccessor {
  @Accessor("BY_CLASS")
  static Map<Class<?>, ArgumentTypeInfo<?, ?>> projecte$getByClass() {
    throw new AssertionError();
  }
}
