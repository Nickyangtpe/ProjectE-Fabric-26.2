package moze_intel.projecte.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMaps.UnmodifiableSortedMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.impl.codec.PECodecHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

public final class CustomEMCParser {
  private static final Path CONFIG = ProjectEConfig.CONFIG_DIR.resolve("custom_emc.json");
  public static CustomEMCParser.CustomEMCFile currentEntries;
  private static boolean dirty = false;

  private static CustomEMCParser.CustomEMCFile createDefault() {
    return new CustomEMCParser.CustomEMCFile(
        new Object2LongLinkedOpenHashMap(), "Use the in-game commands to edit this file");
  }

  public static void init(Provider registries) {
    if (flush(registries)) {
      if (Files.exists(CONFIG)) {
        currentEntries =
            PECodecHelper.<CustomEMCParser.CustomEMCFile>readFromFile(
                    registries, CONFIG, CustomEMCParser.CustomEMCFile.CODEC, "custom emc")
                .orElseGet(CustomEMCParser::createDefault);
      } else {
        currentEntries = createDefault();
        if (!PECodecHelper.writeToFile(
            registries,
            CONFIG,
            CustomEMCParser.CustomEMCFile.CODEC,
            currentEntries,
            "default custom EMC")) {
          dirty = true;
        }
      }
    }
  }

  public static void addToFile(NSSItem toAdd, long emc) {
    if (emc < 0L) {
      throw new IllegalArgumentException("EMC must be non-negative: " + emc);
    }

    long old = currentEntries.entries().put(toAdd, emc);
    if (old == -1L || old != emc) {
      dirty = true;
    }
  }

  public static boolean removeFromFile(NSSItem toRemove) {
    boolean removed = currentEntries.entries().removeLong(toRemove) != -1L;
    if (removed) {
      dirty = true;
    }

    return removed;
  }

  public static boolean flush(Provider registries) {
    if (dirty) {
      if (!PECodecHelper.writeToFile(
          registries, CONFIG, CustomEMCParser.CustomEMCFile.CODEC, currentEntries, "custom EMC")) {
        return false;
      }

      dirty = false;
    }

    return true;
  }

  public record CustomEMCFile(Object2LongSortedMap<NSSItem> entries, @Nullable String comment) {
    public static final Codec<CustomEMCParser.CustomEMCFile> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        ExtraCodecs.NON_EMPTY_STRING
                            .lenientOptionalFieldOf("comment")
                            .forGetter(file -> Optional.ofNullable(file.comment)),
                        IPECodecHelper.INSTANCE
                            .modifiableMap(
                                IPECodecHelper.INSTANCE.lenientKeyUnboundedMap(
                                    NSSItem.CODEC,
                                    IPECodecHelper.INSTANCE.nonNegativeLong().fieldOf("emc")),
                                Object2LongLinkedOpenHashMap::new)
                            .fieldOf("entries")
                            .forGetter(file -> new Object2LongLinkedOpenHashMap<>(file.entries())))
                    .apply(
                        instance,
                        (comment, entries) ->
                            new CustomEMCParser.CustomEMCFile(
                                entries, (String) comment.orElse(null))));

    public CustomEMCFile {
      if (!(entries instanceof UnmodifiableSortedMap)) {
        entries.defaultReturnValue(-1L);
      }
    }
  }
}
