package moze_intel.projecte.api.nss;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.compat.util.NeoForgeExtraCodecs;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNSSTag<TYPE> implements NSSTag {
  private static final Set<NSSTag> createdTags = new HashSet<>();
  @NotNull private final Identifier resourceLocation;
  private final boolean isTag;

  public static Set<NSSTag> getAllCreatedTags() {
    List<NSSTag> sortedTags = new ArrayList<>(createdTags);
    sortedTags.sort(Comparator.comparing(AbstractNSSTag::stableTagSortKey));
    return Collections.unmodifiableSet(new LinkedHashSet<>(sortedTags));
  }

  private static String stableTagSortKey(NSSTag tag) {
    AbstractNSSTag<?> abstractTag = (AbstractNSSTag<?>) tag;
    return abstractTag.getRegistry().key().identifier()
        + "|"
        + abstractTag.getClass().getName()
        + "|"
        + abstractTag.getResourceLocation();
  }

  public static void clearCreatedTags() {
    createdTags.clear();
  }

  protected AbstractNSSTag(@NotNull Identifier resourceLocation, boolean isTag) {
    this.resourceLocation = resourceLocation;
    this.isTag = isTag;
    if (isTag) {
      createdTags.add(this);
    }
  }

  @NotNull
  public Identifier getResourceLocation() {
    return this.resourceLocation;
  }

  @NotNull
  protected abstract Registry<TYPE> getRegistry();

  protected final Optional<Named<TYPE>> getTag(Registry<TYPE> registry) {
    if (this.representsTag()) {
      TagKey<TYPE> tagKey = TagKey.create(registry.key(), this.getResourceLocation());
      return registry.getTags().filter(set -> set.key().equals(tagKey)).findFirst();
    } else {
      return Optional.empty();
    }
  }

  protected abstract NormalizedSimpleStack createNew(Holder<TYPE> var1);

  @Override
  public boolean representsTag() {
    return this.isTag;
  }

  @Override
  public void forEachElement(Consumer<NormalizedSimpleStack> consumer) {
    Optional<Named<TYPE>> tag = this.getTag(this.getRegistry());
    if (tag.isPresent()) {
      for (Holder<TYPE> holder : tag.get()) {
        consumer.accept(this.createNew(holder));
      }
    }
  }

  @Override
  public <CONTEXT, DATA> void forEachElement(
      CONTEXT context, DATA data, TriConsumer<CONTEXT, NormalizedSimpleStack, DATA> consumer) {
    Optional<Named<TYPE>> tag = this.getTag(this.getRegistry());
    if (tag.isPresent()) {
      for (Holder<TYPE> holder : tag.get()) {
        consumer.accept(context, this.createNew(holder), data);
      }
    }
  }

  @Override
  public String toString() {
    Identifier type = this.getRegistry().key().identifier();
    return this.representsTag()
        ? type + " Tag: " + this.getResourceLocation()
        : type + ": " + this.getResourceLocation();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o != null && this.getClass() == o.getClass()) {
      AbstractNSSTag<?> other = (AbstractNSSTag<?>) o;
      return this.representsTag() == other.representsTag()
          && this.getResourceLocation().equals(other.getResourceLocation());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.isTag, this.resourceLocation);
  }

  protected static <TYPE, NSS extends AbstractNSSTag<TYPE>> MapCodec<NSS> createTagCodec(
      AbstractNSSTag.NSSTagConstructor<TYPE, NSS> nssConstructor) {
    return RecordCodecBuilder.mapCodec(
        instance ->
            instance
                .group(
                    IPECodecHelper.INSTANCE
                        .validatePresent(Identifier.CODEC, () -> "Must represent a tag")
                        .fieldOf("tag")
                        .forGetter(nss -> nss.representsTag() ? nss.getResourceLocation() : null))
                .apply(instance, nssConstructor::createTag));
  }

  protected static <TYPE, NSS extends AbstractNSSTag<TYPE>>
      RecordCodecBuilder<NSS, Identifier> idComponent(Registry<?> registry, boolean allowDefault) {
    return Identifier.CODEC
        .validate(
            id -> {
              if (id == null) {
                return DataResult.error(() -> "Must represent a registry id");
              } else if (!registry.containsKey(id)) {
                return DataResult.error(
                    () ->
                        "Registry "
                            + registry.key().identifier()
                            + " does not contain element "
                            + id);
              } else {
                return !allowDefault
                        && registry instanceof DefaultedRegistry<?> defaultedRegistry
                        && id.equals(defaultedRegistry.getDefaultKey())
                    ? DataResult.error(
                        () ->
                            "NormalizedSimpleStack cannot be created for registry "
                                + registry.key().identifier()
                                + " with the default element "
                                + id)
                    : DataResult.success(id);
              }
            })
        .fieldOf("id")
        .forGetter(nss -> nss.representsTag() ? null : nss.getResourceLocation());
  }

  protected static <TYPE, NSS extends AbstractNSSTag<TYPE>> MapCodec<NSS> createCodec(
      Registry<TYPE> registry,
      boolean allowDefault,
      AbstractNSSTag.NSSTagConstructor<TYPE, NSS> nssConstructor) {
    return NeoForgeExtraCodecs.withAlternative(
        createTagCodec(nssConstructor),
        RecordCodecBuilder.mapCodec(
            instance ->
                instance
                    .group(idComponent(registry, allowDefault))
                    .apply(instance, nssConstructor::create)));
  }

  @FunctionalInterface
  protected interface NSSTagConstructor<TYPE, NSS extends AbstractNSSTag<TYPE>> {
    NSS create(Identifier var1, boolean var2);

    default NSS create(Identifier rl) {
      return this.create(rl, false);
    }

    default NSS createTag(Identifier rl) {
      return this.create(rl, true);
    }
  }
}
