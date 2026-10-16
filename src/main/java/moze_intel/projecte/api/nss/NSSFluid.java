package moze_intel.projecte.api.nss;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import moze_intel.projecte.compat.fluids.FluidStack;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public final class NSSFluid extends AbstractDataComponentHolderNSSTag<Fluid> {
  private static final ResourceKey<Fluid> DEFAULT_KEY =
      ResourceKey.create(Registries.FLUID, BuiltInRegistries.FLUID.getDefaultKey());
  public static final MapCodec<NSSFluid> CODEC =
      createCodec(BuiltInRegistries.FLUID, false, NSSFluid::new);

  private NSSFluid(
      @NotNull Identifier resourceLocation,
      boolean isTag,
      @NotNull DataComponentPatch componentsPatch) {
    super(resourceLocation, isTag, componentsPatch);
  }

  @NotNull
  public static NSSFluid createFluid(@NotNull FluidStack stack) {
    return createFluid(stack.builtInRegistryHolder(), stack.getComponentsPatch());
  }

  @NotNull
  public static NSSFluid createFluid(@NotNull Fluid fluid) {
    return createFluid(fluid, DataComponentPatch.EMPTY);
  }

  @NotNull
  public static NSSFluid createFluid(
      @NotNull Fluid fluid, @NotNull DataComponentPatch componentsPatch) {
    return createFluid(fluid.builtInRegistryHolder(), componentsPatch);
  }

  @NotNull
  public static NSSFluid createFluid(@NotNull Holder<Fluid> item) {
    return createFluid(item, DataComponentPatch.EMPTY);
  }

  @NotNull
  public static NSSFluid createFluid(
      @NotNull Holder<Fluid> fluidHolder, @NotNull DataComponentPatch componentsPatch) {
    ResourceKey<Fluid> key = (ResourceKey<Fluid>) fluidHolder.unwrap().left().orElse(null);
    if (key == null) {
      if (!fluidHolder.isBound()) {
        throw new IllegalArgumentException("Can't make an NSSFluid with an unbound direct holder");
      }

      Optional<ResourceKey<Fluid>> registryKey =
          BuiltInRegistries.FLUID.getResourceKey((Fluid) fluidHolder.value());
      if (registryKey.isEmpty()) {
        throw new IllegalArgumentException("Can't make an NSSFluid with an unregistered fluid");
      }

      key = registryKey.get();
    }

    if (key == DEFAULT_KEY) {
      throw new IllegalArgumentException("Can't make NSSFluid with an empty stack");
    } else {
      return createFluid(key.identifier(), componentsPatch);
    }
  }

  @NotNull
  public static NSSFluid createFluid(@NotNull Identifier fluidID) {
    return createFluid(fluidID, DataComponentPatch.EMPTY);
  }

  @NotNull
  public static NSSFluid createFluid(
      @NotNull Identifier fluidID, @NotNull DataComponentPatch componentsPatch) {
    return new NSSFluid(fluidID, false, componentsPatch);
  }

  @NotNull
  public static NSSFluid createTag(@NotNull Identifier tagId) {
    return new NSSFluid(tagId, true, DataComponentPatch.EMPTY);
  }

  @NotNull
  public static NSSFluid createTag(@NotNull TagKey<Fluid> tag) {
    return createTag(tag.location());
  }

  @NotNull
  @Override
  protected Registry<Fluid> getRegistry() {
    return BuiltInRegistries.FLUID;
  }

  protected NSSFluid createNew(Holder<Fluid> fluid) {
    return createFluid(fluid);
  }

  @Override
  public MapCodec<NSSFluid> codec() {
    return CODEC;
  }
}
