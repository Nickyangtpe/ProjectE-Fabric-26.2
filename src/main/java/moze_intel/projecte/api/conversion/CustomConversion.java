package moze_intel.projecte.api.conversion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMaps;
import java.util.List;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.nss.NSSTag;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;

public record CustomConversion(
    int count,
    NormalizedSimpleStack output,
    Object2IntSortedMap<NormalizedSimpleStack> ingredients,
    boolean propagateTags) {
  private static final CustomConversion INVALID =
      new CustomConversion(0, null, Object2IntSortedMaps.emptyMap(), false);
  private static final Codec<Object2IntSortedMap<NormalizedSimpleStack>> INGREDIENT_CODEC =
      IPECodecHelper.INSTANCE.modifiableMap(
          IPECodecHelper.INSTANCE
              .<NormalizedSimpleStack, Integer>unboundedMap(
                  IPECodecHelper.INSTANCE.nssMapCodec(),
                  Codec.INT
                      .validate(
                          value ->
                              value == 0
                                  ? DataResult.error(() -> "Value must not be zero: " + value)
                                  : DataResult.success(value))
                      .optionalFieldOf("amount", 1),
                  (map, nss, amount) -> {
                    map.merge(nss, amount, Integer::sum);
                    return null;
                  })
              .validate(
                  map ->
                      map.isEmpty()
                          ? DataResult.error(() -> "Map must have contents")
                          : DataResult.success(map)),
          Object2IntLinkedOpenHashMap::new);
  private static final MapCodec<CustomConversion> MAP_CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      ExtraCodecs.POSITIVE_INT
                          .optionalFieldOf("count", 1)
                          .forGetter(CustomConversion::count),
                      IPECodecHelper.INSTANCE
                          .nssCodec()
                          .fieldOf("output")
                          .forGetter(CustomConversion::output),
                      INGREDIENT_CODEC
                          .fieldOf("ingredients")
                          .forGetter(CustomConversion::ingredients),
                      Codec.BOOL
                          .optionalFieldOf("propagateTags", false)
                          .forGetter(CustomConversion::propagateTags))
                  .apply(instance, CustomConversion::new));
  public static final Codec<CustomConversion> CODEC = MAP_CODEC.codec();
  private static final Codec<CustomConversion> OR_INVALID_CODEC =
      IPECodecHelper.INSTANCE
          .orElseWithLog(MAP_CODEC, INVALID, () -> "Failed to read conversions: {}")
          .codec();
  public static final Codec<List<CustomConversion>> MODIFIABLE_LIST_CODEC =
      (Codec<List<CustomConversion>>)
          Util.make(
              () -> {
                Codec<List<CustomConversion>> listCodec = OR_INVALID_CODEC.listOf();
                return Codec.of(
                    listCodec,
                    listCodec.map(
                        list ->
                            list.stream()
                                .filter(conversion -> conversion != INVALID)
                                .collect(Util.toMutableList())));
              });

  public CustomConversion(
      int count,
      NormalizedSimpleStack output,
      Object2IntSortedMap<NormalizedSimpleStack> ingredients) {
    this(count, output, ingredients, false);
  }

  public CustomConversion {
    propagateTags = propagateTags && output instanceof NSSTag nssTag && nssTag.representsTag();
  }

  public static CustomConversion getFor(
      int count, NormalizedSimpleStack output, Object2IntMap<NormalizedSimpleStack> ingredients) {
    return new CustomConversion(count, output, new Object2IntLinkedOpenHashMap(ingredients));
  }

  @Override
  public String toString() {
    return "{" + this.count + " * " + this.output + " = " + this.ingredients + "}";
  }
}
