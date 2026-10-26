package moze_intel.projecte.emc.pregenerated;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.codec.MapProcessor;
import moze_intel.projecte.impl.codec.PECodecHelper;
import moze_intel.projecte.impl.codec.PEUnboundedMapCodec;
import net.minecraft.core.HolderLookup.Provider;
import org.jetbrains.annotations.VisibleForTesting;

public class PregeneratedEMC {
  @VisibleForTesting
  static final Codec<Object2LongMap<ItemInfo>> CODEC =
      new PEUnboundedMapCodec<ItemInfo, Long, Object2LongMap<ItemInfo>>(
          ItemInfo.MAP_CODEC,
          IPECodecHelper.INSTANCE.positiveLong().fieldOf("emc"),
          MapProcessor.putIfAbsent(),
          true,
          Object2LongLinkedOpenHashMap::new,
          map -> Object2LongMaps.unmodifiable(map));

  public static Optional<Object2LongMap<ItemInfo>> read(
      Provider registries, Path path, boolean shouldUsePregenerated) {
    return shouldUsePregenerated && Files.isReadable(path)
        ? PECodecHelper.readFromFile(registries, path, CODEC, "pregenerated emc")
        : Optional.empty();
  }

  public static boolean write(Provider registries, Path path, Object2LongMap<ItemInfo> map) {
    return PECodecHelper.writeToFile(registries, path, CODEC, map, "pregenerated emc");
  }
}
