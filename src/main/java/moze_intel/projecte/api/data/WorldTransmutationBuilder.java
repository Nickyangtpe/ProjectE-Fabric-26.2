package moze_intel.projecte.api.data;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.api.world_transmutation.IWorldTransmutation;
import moze_intel.projecte.api.world_transmutation.SimpleWorldTransmutation;
import moze_intel.projecte.api.world_transmutation.WorldTransmutation;
import moze_intel.projecte.api.world_transmutation.WorldTransmutationFile;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldTransmutationBuilder extends BaseFileBuilder<WorldTransmutationBuilder> {
  private static final Logger LOGGER = LogUtils.getLogger();
  private final Set<IWorldTransmutation> transmutationEntries = new LinkedHashSet<>();
  private final Set<BlockState> seenBlockStates = new ReferenceOpenHashSet();
  private final Set<Block> seenBlocks = new ReferenceOpenHashSet();
  private final Set<BlockState> globalSeenBlockStates;
  private final Set<Block> globalSeenBlocks;

  WorldTransmutationBuilder(Set<Block> globalSeenBlocks, Set<BlockState> globalSeenBlockStates) {
    super("World Transmutation");
    this.globalSeenBlocks = globalSeenBlocks;
    this.globalSeenBlockStates = globalSeenBlockStates;
  }

  WorldTransmutationFile build() {
    return new WorldTransmutationFile(this.comment, List.copyOf(this.transmutationEntries));
  }

  private WorldTransmutationBuilder register(@NotNull IWorldTransmutation transmutation) {
    if (!this.transmutationEntries.add(transmutation)) {
      throw new IllegalStateException(
          "World transmutation file contains duplicate transmutations.");
    }

    if (transmutation instanceof SimpleWorldTransmutation simple) {
      Holder<Block> origin = simple.origin();
      Block block = (Block) origin.value();
      if (!this.seenBlocks.add(block)) {
        throw new IllegalStateException(
            "World transmutation file contains multiple simple world transmutations from: "
                + origin.getRegisteredName());
      }

      if (!this.globalSeenBlocks.add(block)) {
        LOGGER.warn(
            "Multiple world transmutation files contain simple world transmutations from: '{}'",
            origin.getRegisteredName());
      }
    } else if (transmutation instanceof WorldTransmutation complex) {
      BlockState state = complex.originState();
      if (!this.seenBlockStates.add(state)) {
        throw new IllegalStateException(
            "World transmutation file contains multiple world transmutations from: " + state);
      }

      if (!this.globalSeenBlockStates.add(state)) {
        LOGGER.warn(
            "Multiple world transmutation files contain world transmutations from: '{}'", state);
      }
    }

    return this;
  }

  public WorldTransmutationBuilder register(BlockState from, BlockState result) {
    if (from.getProperties().isEmpty() && result.getProperties().isEmpty()) {
      throw new IllegalArgumentException(
          "None of the provided states have any properties, use the block based register method.");
    } else if (from == result) {
      throw new IllegalArgumentException(
          "Cannot register a world transmutation from a block to itself.");
    } else {
      return this.register(new WorldTransmutation(from, result));
    }
  }

  public WorldTransmutationBuilder register(
      BlockState from, BlockState result, BlockState altResult) {
    if (from.getProperties().isEmpty()
        && result.getProperties().isEmpty()
        && altResult.getProperties().isEmpty()) {
      throw new IllegalArgumentException(
          "None of the provided states have any properties, use the block based register method.");
    } else if (from != result && from != altResult) {
      return this.register(new WorldTransmutation(from, result, altResult));
    } else {
      throw new IllegalArgumentException(
          "Cannot register a world transmutation from a block to itself.");
    }
  }

  public WorldTransmutationBuilder register(Block from, Block result) {
    return this.register(from.builtInRegistryHolder(), result.builtInRegistryHolder());
  }

  public WorldTransmutationBuilder register(Block from, Block result, Block altResult) {
    return this.register(
        from.builtInRegistryHolder(),
        result.builtInRegistryHolder(),
        altResult.builtInRegistryHolder());
  }

  public WorldTransmutationBuilder register(Holder<Block> from, Holder<Block> result) {
    if (from.is(result)) {
      throw new IllegalArgumentException(
          "Cannot register a world transmutation from a block to itself.");
    } else {
      return this.register(new SimpleWorldTransmutation(from, result));
    }
  }

  public WorldTransmutationBuilder register(
      Holder<Block> from, Holder<Block> result, Holder<Block> altResult) {
    if (!from.is(result) && !from.is(altResult)) {
      return this.register(new SimpleWorldTransmutation(from, result, altResult));
    } else {
      throw new IllegalArgumentException(
          "Cannot register a world transmutation from a block to itself.");
    }
  }

  @SafeVarargs
  public final WorldTransmutationBuilder registerConsecutivePairs(Holder<Block>... blocks) {
    if (blocks.length < 2) {
      throw new IllegalArgumentException(
          "Expected at least two blocks for registering consecutive pairs");
    }

    for (int i = 0; i < blocks.length; i++) {
      Holder<Block> prev = i == 0 ? blocks[blocks.length - 1] : blocks[i - 1];
      Holder<Block> cur = blocks[i];
      Holder<Block> next = i == blocks.length - 1 ? blocks[0] : blocks[i + 1];
      this.register(cur, next, prev);
    }

    return this;
  }

  public WorldTransmutationBuilder registerConsecutivePairs(Block... blocks) {
    if (blocks.length < 2) {
      throw new IllegalArgumentException(
          "Expected at least two blocks for registering consecutive pairs");
    }

    for (int i = 0; i < blocks.length; i++) {
      Block prev = i == 0 ? blocks[blocks.length - 1] : blocks[i - 1];
      Block cur = blocks[i];
      Block next = i == blocks.length - 1 ? blocks[0] : blocks[i + 1];
      this.register(cur, next, prev);
    }

    return this;
  }

  public WorldTransmutationBuilder registerConsecutivePairs(BlockState... states) {
    if (states.length < 2) {
      throw new IllegalArgumentException(
          "Expected at least two states for registering consecutive pairs");
    }

    for (int i = 0; i < states.length; i++) {
      BlockState prev = i == 0 ? states[states.length - 1] : states[i - 1];
      BlockState cur = states[i];
      BlockState next = i == states.length - 1 ? states[0] : states[i + 1];
      this.register(cur, next, prev);
    }

    return this;
  }
}
