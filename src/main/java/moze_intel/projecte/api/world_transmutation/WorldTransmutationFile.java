package moze_intel.projecte.api.world_transmutation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.compat.conditions.ConditionalOps;
import moze_intel.projecte.compat.conditions.WithConditions;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

public record WorldTransmutationFile(
    @Nullable String comment, List<IWorldTransmutation> transmutations) {
  private static final Codec<IWorldTransmutation> TRANSMUTATION_CODEC =
      Codec.either(SimpleWorldTransmutation.CODEC, WorldTransmutation.CODEC)
          .flatComapMap(
              Either::unwrap,
              transmutation -> {
                return DataResult.success(
                    switch (transmutation) {
                      case SimpleWorldTransmutation simple -> Either.left(simple);
                      case WorldTransmutation worldTransmutation ->
                          Either.right(worldTransmutation);
                      default -> throw new MatchException(null, null);
                    });
              });
  private static final Codec<List<IWorldTransmutation>> LIST_CODEC = TRANSMUTATION_CODEC.listOf();
  public static final Codec<WorldTransmutationFile> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      ExtraCodecs.NON_EMPTY_STRING
                          .optionalFieldOf("comment")
                          .forGetter(file -> Optional.ofNullable(file.comment())),
                      LIST_CODEC
                          .optionalFieldOf("transmutations")
                          .forGetter(
                              file -> IPECodecHelper.INSTANCE.ifNotEmpty(file.transmutations())))
                  .apply(
                      instance,
                      (comment, transmutations) ->
                          new WorldTransmutationFile(
                              (String) comment.orElse(null),
                              transmutations.orElseGet(Collections::emptyList))));
  public static final Codec<Optional<WithConditions<WorldTransmutationFile>>> CONDITIONAL_CODEC =
      ConditionalOps.createConditionalCodecWithConditions(CODEC);
}
