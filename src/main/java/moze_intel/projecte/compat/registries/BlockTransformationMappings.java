package moze_intel.projecte.compat.registries;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BlockTransformationMappings {
  private static final Map<Block, Block> OXIDIZABLE_TO_NEXT =
      new LinkedHashMap<>(
          (Map<? extends Block, ? extends Block>) WeatheringCopper.NEXT_BY_BLOCK.get());
  private static final Map<Block, Block> NEXT_TO_OXIDIZABLE =
      reverse((Map<Block, Block>) WeatheringCopper.NEXT_BY_BLOCK.get());
  private static final Map<Block, Block> WAXABLE_TO_WAXED =
      new LinkedHashMap<>((Map<? extends Block, ? extends Block>) HoneycombItem.WAXABLES.get());
  private static final Map<Block, Block> WAXED_TO_WAXABLE =
      reverse((Map<Block, Block>) HoneycombItem.WAXABLES.get());

  private BlockTransformationMappings() {}

  public static synchronized void registerOxidizable(Block lessOxidized, Block nextStage) {
    registerPair(OXIDIZABLE_TO_NEXT, NEXT_TO_OXIDIZABLE, lessOxidized, nextStage, "oxidation");
  }

  public static synchronized void registerWaxable(Block unwaxed, Block waxed) {
    registerPair(WAXABLE_TO_WAXED, WAXED_TO_WAXABLE, unwaxed, waxed, "waxing");
  }

  public static synchronized Map<Block, Block> oxidizableMappings() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(OXIDIZABLE_TO_NEXT));
  }

  public static synchronized Map<Block, Block> waxableMappings() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(WAXABLE_TO_WAXED));
  }

  @Nullable
  public static synchronized BlockState previousOxidationStage(BlockState state) {
    Block previous = NEXT_TO_OXIDIZABLE.get(state.getBlock());
    return previous == null
        ? (BlockState) WeatheringCopper.getPrevious(state).orElse(null)
        : previous.withPropertiesOf(state);
  }

  @Nullable
  public static synchronized Block unwaxedBlock(Block waxed) {
    return WAXED_TO_WAXABLE.get(waxed);
  }

  private static void registerPair(
      Map<Block, Block> forward,
      Map<Block, Block> reverse,
      Block source,
      Block target,
      String kind) {
    if (source == target) {
      throw new IllegalArgumentException("A " + kind + " mapping must connect two distinct blocks");
    }

    Block existingTarget = forward.get(source);
    Block existingSource = reverse.get(target);
    if (existingTarget != null && existingTarget != target) {
      throw new IllegalStateException("Conflicting " + kind + " target registered for " + source);
    }

    if (existingSource != null && existingSource != source) {
      throw new IllegalStateException("Conflicting " + kind + " source registered for " + target);
    }

    forward.put(source, target);
    reverse.put(target, source);
  }

  private static Map<Block, Block> reverse(Map<Block, Block> forward) {
    Map<Block, Block> reverse = new LinkedHashMap<>();
    forward.forEach((source, target) -> reverse.put(target, source));
    return reverse;
  }
}
