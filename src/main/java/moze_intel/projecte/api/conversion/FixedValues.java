package moze_intel.projecte.api.conversion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.util.NeoForgeExtraCodecs;

public record FixedValues(
    Object2LongSortedMap<NormalizedSimpleStack> setValueBefore,
    Object2LongSortedMap<NormalizedSimpleStack> setValueAfter,
    List<CustomConversion> conversions)
    implements IHasConversions {
  private static final Codec<Object2LongSortedMap<NormalizedSimpleStack>> VALUE_CODEC =
      IPECodecHelper.INSTANCE.modifiableMap(
          IPECodecHelper.INSTANCE.lenientKeyUnboundedMap(
              IPECodecHelper.INSTANCE.nssMapCodec(),
              NeoForgeExtraCodecs.withAlternative(
                      IPECodecHelper.INSTANCE.positiveLong(),
                      Codec.stringResolver(
                          val -> val == Long.MIN_VALUE ? "free" : null,
                          str -> str.equalsIgnoreCase("free") ? Long.MIN_VALUE : null))
                  .fieldOf("emc_value")),
          immutableMap -> {
            Object2LongSortedMap<NormalizedSimpleStack> map =
                new Object2LongLinkedOpenHashMap(immutableMap);
            map.defaultReturnValue(-1L);
            return map;
          });
  public static final Codec<FixedValues> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      VALUE_CODEC
                          .optionalFieldOf("before")
                          .forGetter(
                              values ->
                                  IPECodecHelper.INSTANCE.ifNotEmpty(values.setValueBefore())),
                      VALUE_CODEC
                          .optionalFieldOf("after")
                          .forGetter(
                              values -> IPECodecHelper.INSTANCE.ifNotEmpty(values.setValueAfter())),
                      CustomConversion.MODIFIABLE_LIST_CODEC
                          .optionalFieldOf("conversion")
                          .forGetter(
                              values -> IPECodecHelper.INSTANCE.ifNotEmpty(values.conversions())))
                  .apply(
                      instance,
                      (before, after, conversions) ->
                          new FixedValues(
                              before.orElseGet(FixedValues::createEmptyValueMap),
                              after.orElseGet(FixedValues::createEmptyValueMap),
                              (List<CustomConversion>) conversions.orElseGet(ArrayList::new))));

  public FixedValues() {
    this(createEmptyValueMap(), createEmptyValueMap(), new ArrayList<>());
  }

  private static <T> Object2LongSortedMap<T> createEmptyValueMap() {
    Object2LongSortedMap<T> map = new Object2LongLinkedOpenHashMap();
    map.defaultReturnValue(-1L);
    return map;
  }

  public void merge(FixedValues other) {
    this.setValueBefore.putAll(other.setValueBefore());
    this.setValueAfter.putAll(other.setValueAfter());
    this.conversions.addAll(other.conversions());
  }

  public boolean isEmpty() {
    return this.setValueBefore.isEmpty()
        && this.setValueAfter.isEmpty()
        && this.conversions.isEmpty();
  }
}
