package moze_intel.projecte.api.world_transmutation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SimpleWorldTransmutation(
    @NotNull Holder<Block> origin, @NotNull Holder<Block> result, @NotNull Holder<Block> altResult)
    implements IWorldTransmutation {
  private static final Codec<Holder<Block>> BLOCK_CODEC =
      BuiltInRegistries.BLOCK.holderByNameCodec();
  private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Block>> BLOCK_STREAM_CODEC =
      ByteBufCodecs.holderRegistry(Registries.BLOCK);
  public static final Codec<SimpleWorldTransmutation> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      BLOCK_CODEC.fieldOf("origin").forGetter(SimpleWorldTransmutation::origin),
                      BLOCK_CODEC.fieldOf("result").forGetter(SimpleWorldTransmutation::result),
                      BLOCK_CODEC
                          .optionalFieldOf("alt_result")
                          .forGetter(
                              entry ->
                                  entry.hasAlternate()
                                      ? Optional.of(entry.altResult())
                                      : Optional.empty()))
                  .apply(
                      instance,
                      (origin, result, altResult) ->
                          new SimpleWorldTransmutation(origin, result, altResult.orElse(result))));
  public static final StreamCodec<RegistryFriendlyByteBuf, SimpleWorldTransmutation> STREAM_CODEC =
      new StreamCodec<RegistryFriendlyByteBuf, SimpleWorldTransmutation>() {
        @NotNull
        public SimpleWorldTransmutation decode(@NotNull RegistryFriendlyByteBuf buffer) {
          Holder<Block> origin =
              (Holder<Block>) SimpleWorldTransmutation.BLOCK_STREAM_CODEC.decode(buffer);
          Holder<Block> result =
              (Holder<Block>) SimpleWorldTransmutation.BLOCK_STREAM_CODEC.decode(buffer);
          return buffer.readBoolean()
              ? new SimpleWorldTransmutation(
                  origin,
                  result,
                  (Holder<Block>) SimpleWorldTransmutation.BLOCK_STREAM_CODEC.decode(buffer))
              : new SimpleWorldTransmutation(origin, result);
        }

        public void encode(
            @NotNull RegistryFriendlyByteBuf buffer,
            @NotNull SimpleWorldTransmutation transmutation) {
          SimpleWorldTransmutation.BLOCK_STREAM_CODEC.encode(buffer, transmutation.origin());
          SimpleWorldTransmutation.BLOCK_STREAM_CODEC.encode(buffer, transmutation.result());
          boolean hasAlternate = transmutation.hasAlternate();
          buffer.writeBoolean(hasAlternate);
          if (hasAlternate) {
            SimpleWorldTransmutation.BLOCK_STREAM_CODEC.encode(buffer, transmutation.altResult());
          }
        }
      };

  public SimpleWorldTransmutation {
    Objects.requireNonNull(origin, "Origin state cannot be null");
    Objects.requireNonNull(result, "Result state cannot be null");
    Objects.requireNonNull(altResult, "Alternate result state cannot be null");
  }

  public SimpleWorldTransmutation(@NotNull Holder<Block> origin, @NotNull Holder<Block> result) {
    this(origin, result, result);
  }

  @Override
  public boolean hasAlternate() {
    return this.result != this.altResult && !this.result.is(this.altResult);
  }

  @Nullable
  @Override
  public BlockState result(@NotNull BlockState state, boolean isSneaking) {
    if (this.canTransmute(state)) {
      Holder<Block> resultBlock = isSneaking ? this.altResult : this.result;
      return ((Block) resultBlock.value()).withPropertiesOf(state);
    } else {
      return null;
    }
  }

  @Override
  public boolean canTransmute(@NotNull BlockState state) {
    return state.is(this.origin);
  }

  @Override
  public String toString() {
    String representation =
        "Simple World Transmutation from: "
            + this.origin.getRegisteredName()
            + " to: "
            + this.result.getRegisteredName();
    if (this.hasAlternate()) {
      representation =
          representation + ", with secondary output of: " + this.altResult.getRegisteredName();
    }

    return representation;
  }
}
