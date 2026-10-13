package moze_intel.projecte.api.nss;

import com.google.common.base.Objects;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import moze_intel.projecte.api.codec.IPECodecHelper;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

public final class NSSFake implements NormalizedSimpleStack {
  public static final MapCodec<NSSFake> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      ExtraCodecs.NON_EMPTY_STRING
                          .optionalFieldOf("namespace")
                          .forGetter(
                              nssFake ->
                                  IPECodecHelper.INSTANCE.ifNotEmpty(
                                      nssFake.namespace, String::isEmpty)),
                      ExtraCodecs.NON_EMPTY_STRING
                          .fieldOf("description")
                          .forGetter(nssFake -> nssFake.description))
                  .apply(
                      instance,
                      (namespace, description) ->
                          namespace
                              .<NSSFake>map(n -> new NSSFake(n, description))
                              .orElseGet(() -> create(description))));
  private static String currentNamespace = "";
  private final String namespace;
  private final String description;
  private final int cachedHash;

  private NSSFake(String namespace, String description) {
    this.namespace = namespace;
    this.description = description;
    this.cachedHash = Objects.hashCode(new Object[] {this.namespace, this.description});
  }

  public static void resetNamespace() {
    setCurrentNamespace("");
  }

  public static void setCurrentNamespace(@NotNull String ns) {
    currentNamespace = ns;
  }

  @NotNull
  public static NSSFake create(String description) {
    if (description.isEmpty()) {
      throw new IllegalArgumentException("Description must not be empty");
    } else {
      return new NSSFake(currentNamespace, description);
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof NSSFake fake
        && this.description.equals(fake.description)
        && this.namespace.equals(fake.namespace);
  }

  @Override
  public int hashCode() {
    return this.cachedHash;
  }

  @Override
  public String toString() {
    return "NSSFake: " + this.namespace + "/" + this.description;
  }

  @Override
  public MapCodec<NSSFake> codec() {
    return CODEC;
  }
}
