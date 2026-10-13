package moze_intel.projecte.api.conversion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Map.Entry;
import moze_intel.projecte.api.codec.IPECodecHelper;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

public record CustomConversionFile(
    boolean replace,
    @Nullable String comment,
    SequencedMap<String, ConversionGroup> groups,
    FixedValues values) {
  private static final Codec<SequencedMap<String, ConversionGroup>> GROUP_CODEC =
      IPECodecHelper.INSTANCE.modifiableMap(
          Codec.unboundedMap(ExtraCodecs.NON_EMPTY_STRING, ConversionGroup.CODEC),
          LinkedHashMap::new);
  public static final Codec<CustomConversionFile> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.BOOL
                          .optionalFieldOf("replace", false)
                          .forGetter(CustomConversionFile::replace),
                      ExtraCodecs.NON_EMPTY_STRING
                          .optionalFieldOf("comment")
                          .forGetter(file -> Optional.ofNullable(file.comment())),
                      GROUP_CODEC
                          .optionalFieldOf("groups")
                          .forGetter(file -> IPECodecHelper.INSTANCE.ifNotEmpty(file.groups())),
                      FixedValues.CODEC
                          .optionalFieldOf("values")
                          .forGetter(
                              file ->
                                  IPECodecHelper.INSTANCE.ifNotEmpty(
                                      file.values(), FixedValues::isEmpty)))
                  .apply(
                      instance,
                      (replace, comment, groups, values) ->
                          new CustomConversionFile(
                              replace,
                              (String) comment.orElse(null),
                              (SequencedMap<String, ConversionGroup>)
                                  groups.orElseGet(LinkedHashMap::new),
                              values.orElseGet(FixedValues::new))));

  public CustomConversionFile() {
    this(false, null, new LinkedHashMap<>(), new FixedValues());
  }

  public static CustomConversionFile merge(CustomConversionFile left, CustomConversionFile right) {
    if (right.replace) {
      return right;
    }

    for (Entry<String, ConversionGroup> entry : right.groups.entrySet()) {
      left.groups.merge(entry.getKey(), entry.getValue(), ConversionGroup::merge);
    }

    left.values.merge(right.values);
    return left;
  }

  public ConversionGroup getOrAddGroup(String groupName) {
    return this.groups.computeIfAbsent(groupName, name -> new ConversionGroup());
  }
}
