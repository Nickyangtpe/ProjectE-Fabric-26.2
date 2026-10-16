package moze_intel.projecte.compat.common;

import java.util.Arrays;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ToolRuleHelper {
  private ToolRuleHelper() {}

  public static HolderSet<Block> fromTag(TagKey<Block> tag) {
    return BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)
        .getOrThrow(tag);
  }

  public static HolderSet<Block> direct(Block... blocks) {
    return HolderSet.direct(
        Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList());
  }
}
