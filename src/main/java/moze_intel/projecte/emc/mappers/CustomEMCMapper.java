package moze_intel.projecte.emc.mappers;

import it.unimi.dsi.fastutil.objects.Object2LongSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Iterator;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;

@EMCMapper
public class CustomEMCMapper implements IEMCMapper<NormalizedSimpleStack, Long> {
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
