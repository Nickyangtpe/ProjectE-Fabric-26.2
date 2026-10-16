package moze_intel.projecte.api.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import org.jetbrains.annotations.Nullable;

public interface IPECodecHelper {
  IPECodecHelper INSTANCE =
      ServiceLoader.load(IPECodecHelper.class)
          .findFirst()
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "No valid ServiceImpl for IPECodecHelper found, ProjectE may be absent,"
                          + " damaged, or outdated"));

  Codec<NormalizedSimpleStack> nssCodec();

  MapCodec<NormalizedSimpleStack> nssMapCodec();

  Codec<BigInteger> nonNegativeBigInt();

  Codec<BigInteger> positiveBigInt();

  Codec<BigInteger> bigIntRangeWithMessage(
      @Nullable BigInteger var1, @Nullable BigInteger var2, Function<BigInteger, String> var3);

  Codec<Long> nonNegativeLong();

  Codec<Long> positiveLong();

  Codec<Long> longRangeWithMessage(long var1, long var3, Function<Long, String> var5);

  <K, V> Codec<Map<K, V>> lenientKeyUnboundedMap(
      MapCodec<K> var1, MapCodec<V> var2, MapProcessor<K, V> var3);

  default <K, V> Codec<Map<K, V>> lenientKeyUnboundedMap(
      MapCodec<K> keyCodec, MapCodec<V> elementCodec) {
    return this.lenientKeyUnboundedMap(keyCodec, elementCodec, MapProcessor.putIfAbsent());
  }

  <K, V> Codec<Map<K, V>> unboundedMap(MapCodec<K> var1, MapCodec<V> var2, MapProcessor<K, V> var3);

  default <K, V> Codec<Map<K, V>> unboundedMap(MapCodec<K> keyCodec, MapCodec<V> elementCodec) {
    return this.unboundedMap(keyCodec, elementCodec, MapProcessor.putIfAbsent());
  }

  default <T> Codec<T> validatePresent(Codec<T> codec, Supplier<String> errorMessage) {
    return codec.validate(t -> t == null ? DataResult.error(errorMessage) : DataResult.success(t));
  }

  <T> MapCodec<T> orElseWithLog(MapCodec<T> var1, T var2, Supplier<String> var3);

  default <K, V> Codec<Map<K, V>> modifiableMap(Codec<Map<K, V>> codec) {
    return this.modifiableMap(codec, HashMap::new);
  }

  default <K, V, M extends Map<K, V>> Codec<M> modifiableMap(
      Codec<Map<K, V>> codec, Function<Map<K, V>, M> mapConstructor) {
    return (Codec<M>)
        (Codec<?>)
            Codec.of(
                (Codec) codec,
                ((com.mojang.serialization.Decoder) codec).map((Function) mapConstructor));
  }

  default <COLLECTION extends Collection<?>> Optional<COLLECTION> ifNotEmpty(
      COLLECTION collection) {
    return this.ifNotEmpty(collection, Collection::isEmpty);
  }

  default <MAP extends Map<?, ?>> Optional<MAP> ifNotEmpty(MAP map) {
    return this.ifNotEmpty(map, Map::isEmpty);
  }

  default <OBJ> Optional<OBJ> ifNotEmpty(OBJ obj, Predicate<OBJ> emptyCheck) {
    return emptyCheck.test(obj) ? Optional.empty() : Optional.of(obj);
  }
}
