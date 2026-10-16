package moze_intel.projecte.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistryUtils {
  private RegistryUtils() {}

  public static Holder<BlockEntityType<?>> getBEHolder(BlockEntityType<?> type) {
    Holder<BlockEntityType<?>> holder = type.builtInRegistryHolder();
    return holder == null ? BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(type) : holder;
  }
}
