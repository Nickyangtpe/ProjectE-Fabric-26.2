package moze_intel.projecte.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMaps.UnmodifiableSortedMap;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.impl.codec.PECodecHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

public final class CustomEMCParser {
  private static final Path CONFIG = ProjectEConfig.CONFIG_DIR.resolve("custom_emc.json");
  private static final Path BACKUP = CONFIG.resolveSibling("custom_emc.json.bak");
  public static CustomEMCParser.CustomEMCFile currentEntries;
  private static boolean dirty = false;
  private static JsonObject currentDocument = new JsonObject();
  private static List<JsonElement> unresolvedEntries = List.of();
  @Nullable private static JsonObject lastBackedUpDocument;

  private static CustomEMCParser.CustomEMCFile createDefault() {
    return new CustomEMCParser.CustomEMCFile(
        new Object2LongLinkedOpenHashMap(), "Use the in-game commands to edit this file");
  }

  public static synchronized void init(Provider registries) {
    if (!flush(registries)) {
      throw new IllegalStateException(
          "Could not save pending custom EMC values; previous data retained");
    }

    LoadedConfig loaded = readConfig(registries, CONFIG);
    if (loaded != null) {
      install(loaded);
      backup(loaded.document());
      return;
    }

    LoadedConfig recovered = readConfig(registries, BACKUP);
    if (recovered != null) {
      preserveUnreadableFile();
      install(recovered);
      dirty = true;
      if (!flush(registries)) {
        PECore.LOGGER.error(
            "Using the custom EMC backup in memory; the original file could not be restored");
      }
      PECore.LOGGER.warn(
          "Recovered {} custom EMC entries from {}", currentEntries.entries().size(), BACKUP);
      return;
    }

    if (currentEntries != null) {
      preserveUnreadableFile();
      dirty = true;
      PECore.LOGGER.error(
          "Could not reload custom EMC. Retaining all {} in-memory entries",
          currentEntries.entries().size());
      return;
    }

    if (Files.exists(CONFIG) || Files.exists(BACKUP)) {
      throw new IllegalStateException(
          "Custom EMC and its backup could not be read; refusing to replace them with empty data");
    }
    currentEntries = createDefault();
    currentDocument = new JsonObject();
    unresolvedEntries = List.of();
    dirty = true;
    if (!flush(registries)) {
      throw new IllegalStateException("Could not create the initial custom EMC file");
    }
  }

  public static synchronized void addToFile(NSSItem toAdd, long emc) {
    if (emc < 0L) {
      throw new IllegalArgumentException("EMC must be non-negative: " + emc);
    }

    requireLoaded();
    long old = currentEntries.entries().put(toAdd, emc);
    if (old == -1L || old != emc) {
      dirty = true;
    }
  }

  public static synchronized boolean removeFromFile(NSSItem toRemove) {
    requireLoaded();
    boolean removed = currentEntries.entries().removeLong(toRemove) != -1L;
    if (removed) {
      dirty = true;
    }

    return removed;
  }

  public static synchronized boolean flush(Provider registries) {
    if (dirty) {
      requireLoaded();
      DataResult<JsonElement> encoded;
      try {
        encoded =
            CustomEMCFile.CODEC.encodeStart(
                registries.createSerializationContext(JsonOps.INSTANCE), currentEntries);
      } catch (RuntimeException exception) {
        PECore.LOGGER.error("Could not serialize custom EMC; original data retained", exception);
        return false;
      }
      if (encoded.isError()) {
        PECore.LOGGER.error(
            "Could not serialize custom EMC: {}", encoded.error().orElseThrow().message());
        return false;
      }
      JsonObject document = currentDocument.deepCopy();
      JsonObject known = encoded.getOrThrow().getAsJsonObject();
      JsonArray entries = known.getAsJsonArray("entries");
      for (JsonElement unresolved : unresolvedEntries) {
        entries.add(unresolved.deepCopy());
      }
      document.add("entries", entries);
      if (known.has("comment")) {
        document.add("comment", known.get("comment"));
      } else {
        document.remove("comment");
      }
      if (!PECodecHelper.writeJsonToFile(CONFIG, document, "custom EMC")) {
        return false;
      }
      currentDocument = document;
      dirty = false;
      backup(document);
    }

    return true;
  }

  private static void requireLoaded() {
    if (currentEntries == null) {
      throw new IllegalStateException(
          "Custom EMC is unavailable; repair its file or restore a backup before editing it");
    }
  }

  private static void install(LoadedConfig loaded) {
    currentEntries = loaded.values();
    currentDocument = loaded.document();
    unresolvedEntries = loaded.unresolved();
    dirty = false;
  }

  private static void backup(JsonObject document) {
    if (lastBackedUpDocument == null
        || !lastBackedUpDocument.equals(document)
        || !Files.isRegularFile(BACKUP)) {
      if (PECodecHelper.writeJsonToFile(BACKUP, document, "custom EMC backup")) {
        lastBackedUpDocument = document.deepCopy();
      }
    }
  }

  private static void preserveUnreadableFile() {
    if (Files.exists(CONFIG)) {
      Path preserved = CONFIG.resolveSibling("custom_emc.corrupt-" + UUID.randomUUID() + ".json");
      try {
        Files.copy(CONFIG, preserved);
        PECore.LOGGER.warn("Preserved the unreadable custom EMC file at {}", preserved);
      } catch (IOException exception) {
        throw new IllegalStateException(
            "Could not preserve unreadable custom EMC; refusing to overwrite it", exception);
      }
    }
  }

  @Nullable
  private static LoadedConfig readConfig(Provider registries, Path path) {
    if (!Files.exists(path)) {
      return null;
    }
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()
          || !root.getAsJsonObject().has("entries")
          || !root.getAsJsonObject().get("entries").isJsonArray()) {
        throw new IllegalArgumentException("Expected an EMC object with an entries array");
      }
      JsonObject document = root.getAsJsonObject();
      Object2LongLinkedOpenHashMap<NSSItem> entries = new Object2LongLinkedOpenHashMap<>();
      List<JsonElement> unresolved = new ArrayList<>();
      var ops = registries.createSerializationContext(JsonOps.INSTANCE);
      for (JsonElement entry : document.getAsJsonArray("entries")) {
        if (!entry.isJsonObject()) {
          throw new IllegalArgumentException("An EMC entry must be an object");
        }
        DataResult<NSSItem> key = NSSItem.CODEC.decoder().parse(ops, entry);
        if (key.isError()) {
          // An absent mod or temporarily unknown component must not be erased on the next save.
          unresolved.add(entry.deepCopy());
          continue;
        }
        JsonElement rawEmc = entry.getAsJsonObject().get("emc");
        if (rawEmc == null
            || !rawEmc.isJsonPrimitive()
            || !rawEmc.getAsJsonPrimitive().isNumber()) {
          throw new IllegalArgumentException("Invalid EMC for " + key.getOrThrow());
        }
        long value = rawEmc.getAsBigDecimal().toBigIntegerExact().longValueExact();
        if (value < 0L) {
          throw new IllegalArgumentException("Negative EMC for " + key.getOrThrow());
        }
        if (entries.containsKey(key.getOrThrow())) {
          PECore.LOGGER.warn(
              "Duplicate custom EMC entry for {}; using its last value", key.getOrThrow());
        }
        entries.put(key.getOrThrow(), value);
      }
      String comment =
          document.has("comment")
                  && document.get("comment").isJsonPrimitive()
                  && document.get("comment").getAsJsonPrimitive().isString()
              ? document.get("comment").getAsString()
              : null;
      if (comment != null && comment.isEmpty()) {
        comment = null;
      }
      if (!unresolved.isEmpty()) {
        PECore.LOGGER.warn(
            "Preserving {} unresolved custom EMC entries in {} for future recovery",
            unresolved.size(),
            path);
      }
      return new LoadedConfig(
          new CustomEMCFile(entries, comment), document.deepCopy(), List.copyOf(unresolved));
    } catch (IOException | RuntimeException exception) {
      PECore.LOGGER.error(
          "Could not read {}. Existing EMC data will not be replaced with an empty map",
          path,
          exception);
      return null;
    }
  }

  private record LoadedConfig(
      CustomEMCFile values, JsonObject document, List<JsonElement> unresolved) {}

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
