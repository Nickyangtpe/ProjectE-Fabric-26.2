package moze_intel.projecte.api.nss;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import moze_intel.projecte.compat.util.NeoForgeExtraCodecs;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDataComponentHolderNSSTag<TYPE> extends AbstractNSSTag<TYPE>
    implements NSSDataComponentHolder {
  @NotNull private final DataComponentPatch componentsPatch;
  private boolean hasCachedHash;
  private int cachedHashCode;

  protected AbstractDataComponentHolderNSSTag(
      @NotNull Identifier resourceLocation,
      boolean isTag,
      @NotNull DataComponentPatch componentsPatch) {
    super(resourceLocation, isTag);
    this.componentsPatch =
        Objects.requireNonNull(componentsPatch, "Components patch must not be null");
  }

  @NotNull
  @Override
  public DataComponentPatch getComponentsPatch() {
    return this.componentsPatch;
  }

  @Override
  public String toString() {
    return this.componentsPatch != null && !this.componentsPatch.isEmpty()
        ? super.toString() + " " + this.componentsPatch
        : super.toString();
  }

  @Override
  public boolean equals(Object o) {
    return o == this
        || super.equals(o)
            && Objects.equals(
                this.componentsPatch, ((AbstractDataComponentHolderNSSTag) o).componentsPatch);
  }

  @Override
  public int hashCode() {
    if (this.componentsPatch == null) {
      this.hasCachedHash = true;
      this.cachedHashCode = Objects.hash(super.hashCode(), DataComponentPatch.EMPTY);
    } else if (!this.hasCachedHash) {
      this.hasCachedHash = true;
      this.cachedHashCode = Objects.hash(super.hashCode(), this.componentsPatch);
    }

    return this.cachedHashCode;
  }

  protected static <TYPE, NSS extends AbstractDataComponentHolderNSSTag<TYPE>>
      MapCodec<NSS> createCodec(
          Registry<TYPE> registry,
          boolean allowDefault,
          AbstractDataComponentHolderNSSTag.DataComponentHolderNSSConstructor<TYPE, NSS>
              nssConstructor) {
    return NeoForgeExtraCodecs.withAlternative(
        createTagCodec(nssConstructor),
        RecordCodecBuilder.mapCodec(
            instance ->
                instance
                    .group(
                        idComponent(registry, allowDefault),
                        DataComponentPatch.CODEC
                            .optionalFieldOf("data", DataComponentPatch.EMPTY)
                            .forGetter(AbstractDataComponentHolderNSSTag::getComponentsPatch))
                    .apply(instance, nssConstructor::create)));
  }

  @FunctionalInterface
  protected interface DataComponentHolderNSSConstructor<
          TYPE, NSS extends AbstractDataComponentHolderNSSTag<TYPE>>
      extends AbstractNSSTag.NSSTagConstructor<TYPE, NSS> {
    NSS create(Identifier var1, boolean var2, @NotNull DataComponentPatch var3);

    default NSS create(Identifier rl, boolean isTag) {
      return this.create(rl, isTag, DataComponentPatch.EMPTY);
    }

    default NSS create(Identifier rl, @NotNull DataComponentPatch componentsPatch) {
      return this.create(rl, false, componentsPatch);
    }
  }
}
