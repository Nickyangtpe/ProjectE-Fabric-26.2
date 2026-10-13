package moze_intel.projecte.gameObjs;

import moze_intel.projecte.PECore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class PETags {
  private PETags() {}

  public static class BlockEntities {
    public static final TagKey<BlockEntityType<?>> BLACKLIST_TIME_WATCH =
        tag("blacklist/time_watch");

    private BlockEntities() {}

    private static TagKey<BlockEntityType<?>> tag(String name) {
      return TagKey.create(Registries.BLOCK_ENTITY_TYPE, PECore.rl(name));
    }
  }

  public static class Blocks {
    public static final TagKey<Block> BLACKLIST_HARVEST = tag("blacklist/harvest");
    public static final TagKey<Block> OVERRIDE_PLANTABLE = tag("override/plantable");
    public static final TagKey<Block> BLACKLIST_TIME_WATCH = tag("blacklist/time_watch");
    public static final TagKey<Block> VEIN_SHOVEL = tag("vein/shovel");
    public static final TagKey<Block> FARMING_OVERRIDE = tag("farming_override");
    public static final TagKey<Block> NEEDS_DARK_MATTER_TOOL = tag("needs_dark_matter_tool");
    public static final TagKey<Block> NEEDS_RED_MATTER_TOOL = tag("needs_red_matter_tool");
    public static final TagKey<Block> INCORRECT_FOR_DARK_MATTER_TOOL =
        tag("incorrect_for_dark_matter_tool");
    public static final TagKey<Block> INCORRECT_FOR_RED_MATTER_TOOL =
        tag("incorrect_for_red_matter_tool");
    public static final TagKey<Block> MINEABLE_WITH_PE_KATAR = tag("mineable/katar");
    public static final TagKey<Block> MINEABLE_WITH_PE_HAMMER = tag("mineable/hammer");
    public static final TagKey<Block> MINEABLE_WITH_PE_MORNING_STAR = tag("mineable/morning_star");
    public static final TagKey<Block> MINEABLE_WITH_PE_SHEARS = tag("mineable/shears");
    public static final TagKey<Block> MINEABLE_WITH_PE_SWORD = tag("mineable/sword");
    public static final TagKey<Block> MINEABLE_WITH_HAMMER = commonTag("mineable/hammer");
    public static final TagKey<Block> MINEABLE_WITH_KATAR = commonTag("mineable/katar");
    public static final TagKey<Block> MINEABLE_WITH_MORNING_STAR =
        commonTag("mineable/morning_star");

    private Blocks() {}

    private static TagKey<Block> tag(String name) {
      return TagKey.create(Registries.BLOCK, PECore.rl(name));
    }

    private static TagKey<Block> commonTag(String name) {
      return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", name));
    }
  }

  public static class Entities {
    public static final TagKey<EntityType<?>> BLACKLIST_SWRG = tag("blacklist/swrg");
    public static final TagKey<EntityType<?>> BLACKLIST_INTERDICTION =
        tag("blacklist/interdiction");
    public static final TagKey<EntityType<?>> RANDOMIZER_PEACEFUL = tag("randomizer/peaceful");
    public static final TagKey<EntityType<?>> RANDOMIZER_HOSTILE = tag("randomizer/hostile");

    private Entities() {}

    private static TagKey<EntityType<?>> tag(String name) {
      return TagKey.create(Registries.ENTITY_TYPE, PECore.rl(name));
    }
  }

  public static class Items {
    public static final TagKey<Item> ALCHEMICAL_BAGS = tag("alchemical_bags");
    public static final TagKey<Item> KLEIN_STARS = tag("klein_stars");
    public static final TagKey<Item> COLLECTORS = tag("collectors");
    public static final TagKey<Item> RELAYS = tag("relays");
    public static final TagKey<Item> MATTER_FURNACES = tag("matter_furnaces");
    public static final TagKey<Item> COLLECTOR_FUEL = tag("collector_fuel");
    public static final TagKey<Item> DATA_COMPONENT_WHITELIST = tag("data_component_whitelist");
    public static final TagKey<Item> COVALENCE_DUST = tag("covalence_dust");
    public static final TagKey<Item> IGNORE_MISSING_EMC = tag("ignore_missing_emc");
    public static final TagKey<Item> PLANTABLE_SEEDS = tag("plantable_seeds");
    public static final TagKey<Item> TRANSMUTATION_TABLET = tag("transmutation_tablets");
    public static final TagKey<Item> CURIOS_BELT = curiosTag("belt");
    public static final TagKey<Item> CURIOS_KLEIN_STAR = curiosTag("klein_star");
    public static final TagKey<Item> CURIOS_TRANSMUTATION_TABLET =
        curiosTag("transmutation_tablet");
    public static final TagKey<Item> CURIOS_NECKLACE = curiosTag("necklace");
    public static final TagKey<Item> CURIOS_RING = curiosTag("ring");
    public static final TagKey<Item> TOOLS_HAMMERS = commonTag("tools/hammers");
    public static final TagKey<Item> TOOLS_KATARS = commonTag("tools/katars");
    public static final TagKey<Item> TOOLS_MORNING_STARS = commonTag("tools/morning_stars");

    private Items() {}

    private static TagKey<Item> tag(String name) {
      return TagKey.create(Registries.ITEM, PECore.rl(name));
    }

    private static TagKey<Item> curiosTag(String name) {
      return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", name));
    }

    private static TagKey<Item> commonTag(String name) {
      return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", name));
    }
  }

  public static class Potions {
    public static final TagKey<Potion> IGNORE_MISSING_EMC = tag("ignore_missing_emc");

    private Potions() {}

    private static TagKey<Potion> tag(String name) {
      return TagKey.create(Registries.POTION, PECore.rl(name));
    }
  }
}
