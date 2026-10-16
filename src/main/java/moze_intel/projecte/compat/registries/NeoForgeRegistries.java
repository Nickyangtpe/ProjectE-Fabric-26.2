package moze_intel.projecte.compat.registries;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.compat.conditions.ICondition;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class NeoForgeRegistries {
  public static final Registry<MapCodec<? extends ICondition>> CONDITION_CODECS =
      FabricRegistryBuilder.create(NeoForgeRegistries.Keys.CONDITION_CODECS).buildAndRegister();

  private NeoForgeRegistries() {}

  public static final class Keys {
    public static final ResourceKey<Registry<MapCodec<? extends ICondition>>> CONDITION_CODECS =
        key("condition_codec");

    private Keys() {}

    private static <T> ResourceKey<Registry<T>> key(String path) {
      return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("projecte", path));
    }
  }
}
