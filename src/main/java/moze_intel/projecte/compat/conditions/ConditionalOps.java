package moze_intel.projecte.compat.conditions;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ConditionalOps {
  private static final String NEOFORGE_CONDITIONS = "neoforge:conditions";
  private static final String FORGE_CONDITIONS = "forge:conditions";
  private static final String NEOFORGE_VALUE = "neoforge:value";
  private static final Codec<ICondition> CONDITION_CODEC =
      Codec.PASSTHROUGH.xmap(
          dynamic ->
              new JsonBackedCondition(
                  (JsonElement) dynamic.convert(JsonOps.INSTANCE).getValue(),
                  "<conditional data resource>"),
          condition -> {
            if (condition instanceof JsonBackedCondition jsonCondition) {
              return new Dynamic(JsonOps.INSTANCE, jsonCondition.definition());
            } else {
              throw new IllegalArgumentException(
                  "Cannot serialize an unsupported ProjectE condition implementation: "
                      + condition.getClass().getName());
            }
          });

  private ConditionalOps() {}

  public static <T> Codec<Optional<WithConditions<T>>> createConditionalCodecWithConditions(
      final Codec<T> codec) {
    final Codec<List<ICondition>> conditionsCodec = CONDITION_CODEC.listOf();
    return new Codec<Optional<WithConditions<T>>>() {
      public <A> DataResult<Pair<Optional<WithConditions<T>>, A>> decode(
          DynamicOps<A> ops, A input) {
        return ConditionalOps.decode(codec, conditionsCodec, ops, input);
      }

      public <A> DataResult<A> encode(
          Optional<WithConditions<T>> input, DynamicOps<A> ops, A prefix) {
        return ConditionalOps.encode(codec, conditionsCodec, input, ops, prefix);
      }

      @Override
      public String toString() {
        return "ConditionalCodec[" + codec + "]";
      }
    };
  }

  private static <T, A> DataResult<Pair<Optional<WithConditions<T>>, A>> decode(
      Codec<T> codec, Codec<List<ICondition>> conditionsCodec, DynamicOps<A> ops, A input) {
    MapLike<A> map = (MapLike<A>) ops.getMap(input).result().orElse(null);
    A valueInput = input;
    A conditionsInput = null;
    if (map != null) {
      A neoforgeValue = (A) map.get("neoforge:value");
      A neoforgeConditions = (A) map.get("neoforge:conditions");
      A forgeConditions = (A) map.get("forge:conditions");
      A legacyValue = (A) map.get("value");
      A legacyConditions = (A) map.get("conditions");
      if (neoforgeValue != null) {
        valueInput = neoforgeValue;
      } else if (neoforgeConditions == null
          && forgeConditions == null
          && legacyValue != null
          && legacyConditions != null) {
        valueInput = legacyValue;
        conditionsInput = legacyConditions;
      }

      if (conditionsInput == null) {
        conditionsInput = neoforgeConditions != null ? neoforgeConditions : forgeConditions;
      }
    }

    A payload = valueInput;
    DataResult<List<ICondition>> conditions =
        conditionsInput == null
            ? DataResult.success(List.of())
            : conditionsCodec.parse(ops, conditionsInput);
    return conditions.flatMap(
        parsedConditions ->
            codec
                .parse(ops, payload)
                .map(
                    value ->
                        Pair.of(
                            Optional.of(
                                new WithConditions<>(
                                    value, parsedConditions.toArray(ICondition[]::new))),
                            ops.empty())));
  }

  private static <T, A> DataResult<A> encode(
      Codec<T> codec,
      Codec<List<ICondition>> conditionsCodec,
      Optional<WithConditions<T>> input,
      DynamicOps<A> ops,
      A prefix) {
    if (input.isEmpty()) {
      return ops.mapBuilder().build(prefix);
    }

    WithConditions<T> value = input.get();
    return value.conditions().length == 0
        ? codec.encode(value.value(), ops, prefix)
        : codec
            .encodeStart(ops, value.value())
            .flatMap(
                encodedValue ->
                    conditionsCodec
                        .encodeStart(ops, Arrays.asList(value.conditions()))
                        .flatMap(
                            encodedConditions -> {
                              RecordBuilder<A> builder = ops.mapBuilder();
                              Optional<MapLike<A>> valueMap = ops.getMap(encodedValue).result();
                              if (valueMap.isPresent()) {
                                valueMap
                                    .get()
                                    .entries()
                                    .forEach(
                                        entry -> builder.add(entry.getFirst(), entry.getSecond()));
                                builder.add("neoforge:conditions", encodedConditions);
                              } else {
                                builder.add("neoforge:conditions", encodedConditions);
                                builder.add("neoforge:value", encodedValue);
                              }

                              return builder.build(prefix);
                            }));
  }
}
