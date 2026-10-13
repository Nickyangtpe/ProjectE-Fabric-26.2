package moze_intel.projecte.api;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.registries.RegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class ProjectERegistries {
  public static final ResourceKey<Registry<MapCodec<? extends NormalizedSimpleStack>>>
      NSS_SERIALIZER_NAME = ResourceKey.createRegistryKey(rl("nss_serializer"));
  public static final Registry<MapCodec<? extends NormalizedSimpleStack>> NSS_SERIALIZER =
      new RegistryBuilder<MapCodec<? extends NormalizedSimpleStack>>(NSS_SERIALIZER_NAME)
          .defaultKey(rl("item"))
          .create();

  private ProjectERegistries() {}

  private static Identifier rl(String path) {
    return Identifier.fromNamespaceAndPath("projecte", path);
  }
}
