package moze_intel.projecte.api.conversion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

public record ConversionGroup(@Nullable String comment, List<CustomConversion> conversions)
    implements IHasConversions {
  public static final Codec<ConversionGroup> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      ExtraCodecs.NON_EMPTY_STRING
                          .optionalFieldOf("comment")
                          .forGetter(group -> Optional.ofNullable(group.comment())),
                      CustomConversion.MODIFIABLE_LIST_CODEC
                          .fieldOf("conversions")
                          .forGetter(ConversionGroup::conversions))
                  .apply(
                      instance,
                      (comment, conversions) ->
                          new ConversionGroup((String) comment.orElse(null), conversions)));

  public ConversionGroup() {
    this(null, new ArrayList<>());
  }

  public int size() {
    return this.conversions.size();
  }

  public ConversionGroup merge(ConversionGroup other) {
    this.conversions.addAll(other.conversions());
    return this;
  }
}
