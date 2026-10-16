package moze_intel.projecte.compat.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;

public final class NeoForgeExtraCodecs {
  private NeoForgeExtraCodecs() {}

  public static <T> Codec<T> withAlternative(final Codec<T> primary, final Codec<T> alternative) {
    return new Codec<T>() {
      public <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
        DataResult<Pair<T, O>> result = primary.decode(ops, input);
        return result.error().isEmpty() ? result : alternative.decode(ops, input);
      }

      public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
        DataResult<O> result = primary.encode(input, ops, prefix);
        return result.error().isEmpty() ? result : alternative.encode(input, ops, prefix);
      }

      @Override
      public String toString() {
        return "WithAlternative[" + primary + ", " + alternative + "]";
      }
    };
  }

  public static <T> MapCodec<T> withAlternative(
      final MapCodec<T> primary, final MapCodec<T> alternative) {
    return new MapCodec<T>() {
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.concat(primary.keys(ops), alternative.keys(ops)).distinct();
      }

      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        DataResult<T> result = primary.decode(ops, input);
        return result.error().isEmpty() ? result : alternative.decode(ops, input);
      }

      public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        DataResult<O> result = primary.encode(input, ops, prefix).build(ops.empty());
        return result.error().isEmpty()
            ? primary.encode(input, ops, prefix)
            : alternative.encode(input, ops, prefix);
      }

      public String toString() {
        return "WithAlternativeMap[" + primary + ", " + alternative + "]";
      }
    };
  }
}
