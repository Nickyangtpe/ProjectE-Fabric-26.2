package moze_intel.projecte.api.world_transmutation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import moze_intel.projecte.compat.util.NeoForgeExtraCodecs;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public record WorldTransmutation(
    @NotNull BlockState originState, @NotNull BlockState result, @NotNull BlockState altResult)
    implements IWorldTransmutation {
  static final String ORIGIN_KEY = "origin";
  static final String RESULT_KEY = "result";
  static final String ALT_RESULT_KEY = "alt_result";
  private static final Codec<BlockState> STATE_CODEC =
      NeoForgeExtraCodecs.withAlternative(
          BuiltInRegistries.BLOCK
              .byNameCodec()
              .flatXmap(
                  block -> DataResult.success(block.defaultBlockState()),
                  state ->
                      state.getProperties().isEmpty()
                          ? DataResult.success(state.getBlock())
                          : DataResult.error(
                              () ->
                                  "Flattened state codec cannot be used for blocks that define any"
                                      + " properties.")),
          BlockState.CODEC);
  private static final StreamCodec<ByteBuf, BlockState> STATE_STREAM_CODEC =
      ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY);
  public static final Codec<WorldTransmutation> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      STATE_CODEC.fieldOf("origin").forGetter(WorldTransmutation::originState),
                      STATE_CODEC.fieldOf("result").forGetter(WorldTransmutation::result),
                      STATE_CODEC
                          .optionalFieldOf("alt_result")
                          .forGetter(
                              entry ->
                                  entry.hasAlternate()
                                      ? Optional.of(entry.altResult())
                                      : Optional.empty()))
                  .apply(
                      instance,
                      (origin, result, altResult) ->
                          new WorldTransmutation(origin, result, altResult.orElse(result))));
  public static final StreamCodec<ByteBuf, WorldTransmutation> STREAM_CODEC =
      new StreamCodec<ByteBuf, WorldTransmutation>() {
        @NotNull
        public WorldTransmutation decode(@NotNull ByteBuf buffer) {
          BlockState origin = (BlockState) WorldTransmutation.STATE_STREAM_CODEC.decode(buffer);
          BlockState result = (BlockState) WorldTransmutation.STATE_STREAM_CODEC.decode(buffer);
          return buffer.readBoolean()
              ? new WorldTransmutation(
                  origin, result, (BlockState) WorldTransmutation.STATE_STREAM_CODEC.decode(buffer))
              : new WorldTransmutation(origin, result);
        }

        public void encode(@NotNull ByteBuf buffer, @NotNull WorldTransmutation transmutation) {
          WorldTransmutation.STATE_STREAM_CODEC.encode(buffer, transmutation.originState());
          WorldTransmutation.STATE_STREAM_CODEC.encode(buffer, transmutation.result());
          boolean hasAlternate = transmutation.hasAlternate();
          buffer.writeBoolean(hasAlternate);
          if (hasAlternate) {
            WorldTransmutation.STATE_STREAM_CODEC.encode(buffer, transmutation.altResult());
          }
        }
      };

  public WorldTransmutation {
    Objects.requireNonNull(originState, "Origin state cannot be null");
    Objects.requireNonNull(result, "Result state cannot be null");
    Objects.requireNonNull(altResult, "Alternate result state cannot be null");
  }

  public WorldTransmutation(@NotNull BlockState originState, @NotNull BlockState result) {
    this(originState, result, result);
  }

  @NotNull
  public static IWorldTransmutation of(@NotNull BlockState origin, @NotNull BlockState result) {
    return origin.getProperties().isEmpty() && result.getProperties().isEmpty()
        ? new SimpleWorldTransmutation(origin.typeHolder(), result.typeHolder())
        : new WorldTransmutation(origin, result);
  }

  @NotNull
  public static IWorldTransmutation of(
      @NotNull BlockState origin, @NotNull BlockState result, @NotNull BlockState altResult) {
    return origin.getProperties().isEmpty()
            && result.getProperties().isEmpty()
            && altResult.getProperties().isEmpty()
        ? new SimpleWorldTransmutation(
            origin.typeHolder(), result.typeHolder(), altResult.typeHolder())
        : new WorldTransmutation(origin, result, altResult);
  }

  @Override
  public Holder<Block> origin() {
    return this.originState.typeHolder();
  }

  @Override
  public boolean hasAlternate() {
    return this.altResult != this.result;
  }

  @Override
  public BlockState result(@NotNull BlockState state, boolean isSneaking) {
    if (this.canTransmute(state)) {
      return isSneaking ? this.altResult : this.result;
    } else {
      return null;
    }
  }

  @Override
  public boolean canTransmute(@NotNull BlockState state) {
    return state == this.originState;
  }

  @Override
  public String toString() {
    String representation =
        "World Transmutation from: '" + this.originState + "' to: '" + this.result + "'";
    if (this.hasAlternate()) {
      representation = representation + ", with secondary output of: '" + this.altResult + "'";
    }

    return representation;
  }
}
