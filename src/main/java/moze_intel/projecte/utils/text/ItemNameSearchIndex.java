package moze_intel.projecte.utils.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import moze_intel.projecte.PECore;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ItemNameSearchIndex implements SimpleSynchronousResourceReloadListener {
  private static final Identifier RELOAD_ID = PECore.rl("item_name_search");
  private static volatile Map<String, Set<String>> namesByTranslationKey = Map.of();

  public Identifier getFabricId() {
    return RELOAD_ID;
  }

  public void onResourceManagerReload(ResourceManager resourceManager) {
    Map<String, Set<String>> mutableIndex = new HashMap<>();
    Map<Identifier, Resource> languageFiles =
        resourceManager.listResources("lang", id -> id.getPath().endsWith(".json"));

    for (Entry<Identifier, Resource> entry : languageFiles.entrySet()) {
      try (BufferedReader reader = entry.getValue().openAsReader()) {
        JsonElement root = JsonParser.parseReader(reader);
        if (root.isJsonObject()) {
          for (Entry<String, JsonElement> translation : root.getAsJsonObject().entrySet()) {
            String key = translation.getKey();
            if ((key.startsWith("item.") || key.startsWith("block."))
                && translation.getValue().isJsonPrimitive()
                && translation.getValue().getAsJsonPrimitive().isString()) {
              String name = translation.getValue().getAsString().strip().toLowerCase(Locale.ROOT);
              if (!name.isEmpty()) {
                mutableIndex.computeIfAbsent(key, ignored -> new HashSet<>()).add(name);
              }
            }
          }
        }
      } catch (IOException | JsonParseException e) {
        PECore.LOGGER.warn("Unable to index item translations from {}", entry.getKey(), e);
      }
    }

    Map<String, Set<String>> immutableIndex = new HashMap<>(mutableIndex.size());
    mutableIndex.forEach(
        (keyx, names) ->
            immutableIndex.put(keyx, Set.copyOf((Collection<? extends String>) names)));
    namesByTranslationKey = Map.copyOf(immutableIndex);
    PECore.LOGGER.debug(
        "Indexed localized names for {} item/block translation keys", namesByTranslationKey.size());
  }

  public static boolean contains(String translationKey, String search) {
    return translationKey != null && search != null && !search.isEmpty()
        ? namesByTranslationKey.getOrDefault(translationKey, Set.of()).stream()
            .anyMatch(name -> name.contains(search))
        : false;
  }
}
