package moze_intel.projecte.compat.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class Tags {
  private Tags() {}

  private static TagKey<Block> commonBlock(String path) {
    return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
  }

  private static TagKey<Item> commonItem(String path) {
    return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
  }

  public static final class Blocks {
    public static final TagKey<Block> ORES = Tags.commonBlock("ores");

    private Blocks() {}
  }

  public static final class Items {
    public static final TagKey<Item> ORES = Tags.commonItem("ores");
    public static final TagKey<Item> RAW_MATERIALS = Tags.commonItem("raw_materials");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_COPPER =
        Tags.commonItem("storage_blocks/raw_copper");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_GOLD =
        Tags.commonItem("storage_blocks/raw_gold");
    public static final TagKey<Item> STORAGE_BLOCKS_RAW_IRON =
        Tags.commonItem("storage_blocks/raw_iron");
    public static final TagKey<Item> TOOLS_SHIELD = Tags.commonItem("tools/shield");

    private Items() {}
  }
}
