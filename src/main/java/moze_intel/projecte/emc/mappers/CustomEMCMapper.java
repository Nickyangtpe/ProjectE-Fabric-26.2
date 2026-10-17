package moze_intel.projecte.emc.mappers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMaps;
import java.io.IOException;
import java.io.Reader;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Iterator;
import java.util.Map;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@EMCMapper
public class CustomEMCMapper implements IEMCMapper<NormalizedSimpleStack, Long> {
  private static final FileToIdConverter SIMPLE_EMC_LISTER = FileToIdConverter.json("emc");

  @Override
  public void addMappings(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      ReloadableServerResources serverResources,
      RegistryAccess registryAccess,
      ResourceManager resourceManager) {
    Iterator<Entry<NSSItem>> iterator =
        Object2LongSortedMaps.fastIterator(CustomEMCParser.currentEntries.entries());

    while (iterator.hasNext()) {
      Entry<NSSItem> entry = iterator.next();
      NSSItem item = (NSSItem) entry.getKey();
      long emc = entry.getLongValue();
      PECore.debugLog("Adding custom EMC value for {}: {}", item, emc);
      item.forSelfAndEachElement(mapper, emc, IMappingCollector::setValueBefore);
    }

    loadSimpleDatapackValues(mapper, resourceManager);
  }

  /**
   * Loads the lightweight public format: data/<namespace>/emc/*.json.
   *
   * <p>This deliberately lives in the mapper instead of requiring a ProjectE API dependency, so
   * any Fabric mod can ship EMC values in its own data pack.
   */
  private static void loadSimpleDatapackValues(
      IMappingCollector<NormalizedSimpleStack, Long> mapper, ResourceManager resourceManager) {
    for (Map.Entry<Identifier, java.util.List<Resource>> entry :
        SIMPLE_EMC_LISTER.listMatchingResourceStacks(resourceManager).entrySet()) {
      Identifier resourceId = entry.getKey();
      for (Resource resource : entry.getValue()) {
        try (Reader reader = resource.openAsReader()) {
          JsonElement root = JsonParser.parseReader(reader);
          if (!root.isJsonObject()) {
            PECore.LOGGER.error("Ignoring EMC file {}: root must be a JSON object", resourceId);
            continue;
          }
          JsonObject object = root.getAsJsonObject();
          JsonObject values = object.has("values") && object.get("values").isJsonObject()
              ? object.getAsJsonObject("values")
              : object;
          for (Map.Entry<String, JsonElement> value : values.entrySet()) {
            if (value.getKey().equals("comment") || value.getKey().equals("values")) {
              continue;
            }
            if (!value.getValue().isJsonPrimitive()
                || !value.getValue().getAsJsonPrimitive().isNumber()) {
              PECore.LOGGER.error(
                  "Ignoring EMC entry {} in {}: value must be a non-negative integer",
                  value.getKey(), resourceId);
              continue;
            }
            long emc = value.getValue().getAsLong();
            if (emc < 0L) {
              PECore.LOGGER.error(
                  "Ignoring EMC entry {} in {}: value must be non-negative",
                  value.getKey(), resourceId);
              continue;
            }
            String id = value.getKey();
            boolean tag = id.startsWith("#");
            Identifier identifier = Identifier.parse(tag ? id.substring(1) : id);
            NSSItem item = tag ? NSSItem.createTag(identifier) : NSSItem.createItem(identifier);
            PECore.debugLog("Adding datapack EMC value for {}: {}", id, emc);
            item.forSelfAndEachElement(mapper, emc, IMappingCollector::setValueBefore);
          }
        } catch (IOException | RuntimeException exception) {
          PECore.LOGGER.error("Could not load simple EMC file {}", resourceId, exception);
        }
      }
    }
  }

  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CUSTOM_EMC_MAPPER.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CUSTOM_EMC_MAPPER.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CUSTOM_EMC_MAPPER.tooltip();
  }
}
