package moze_intel.projecte.emc;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.event.EMCRemapEvent;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import moze_intel.projecte.api.mapper.collector.IExtendedMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.MappingConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.arithmetic.HiddenBigFractionArithmetic;
import moze_intel.projecte.emc.collector.DumpToFileCollector;
import moze_intel.projecte.emc.collector.LongToBigFractionCollector;
import moze_intel.projecte.emc.components.DataComponentManager;
import moze_intel.projecte.emc.generator.BigFractionToLongGenerator;
import moze_intel.projecte.emc.mappers.CustomEMCMapper;
import moze_intel.projecte.emc.mappers.TagMapper;
import moze_intel.projecte.emc.pregenerated.PregeneratedEMC;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import moze_intel.projecte.network.packets.to_client.SyncEmcPKT;
import moze_intel.projecte.utils.AnnotationHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.math3.fraction.BigFraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class EMCMappingHandler {
  private static final List<IEMCMapper<NormalizedSimpleStack, Long>> mappers = new ArrayList<>();
  @Nullable private static volatile Object2LongMap<ItemInfo> emc;
  private static volatile int loadIndex = -1;

  public static void loadMappers() {
    if (mappers.isEmpty()) {
      mappers.addAll(AnnotationHelper.getEMCMappers());
      mappers.add(new TagMapper());
      // Explicit values override automatic defaults/blacklists; tag inheritance runs last.
      mappers.sort(Comparator.comparingInt(EMCMappingHandler::getMappingPriority));
      MappingConfig.setup(mappers, DataComponentManager.loadProcessors());
    }
  }

  private static int getMappingPriority(IEMCMapper<NormalizedSimpleStack, Long> mapper) {
    if (mapper instanceof TagMapper) {
      return 2;
    }
    return mapper instanceof CustomEMCMapper ? 1 : 0;
  }

  public static void map(
      ReloadableServerResources serverResources,
      RegistryAccess registryAccess,
      ResourceManager resourceManager) {
    // Build a replacement without making the live price table disappear during collection.
    SimpleGraphMapper<NormalizedSimpleStack, BigFraction, IValueArithmetic<BigFraction>> mapper =
        new SimpleGraphMapper<>(new HiddenBigFractionArithmetic());
    BigFractionToLongGenerator<NormalizedSimpleStack> valueGenerator =
        new BigFractionToLongGenerator<>(mapper);
    IExtendedMappingCollector<NormalizedSimpleStack, Long, IValueArithmetic<BigFraction>>
        mappingCollector = new LongToBigFractionCollector<>(mapper);
    if (MappingConfig.dumpToFile()) {
      mappingCollector =
          new DumpToFileCollector<>(
              ProjectEConfig.CONFIG_DIR.resolve("mapping_dump.json"), mappingCollector);
    }

    // Explicit server values must never be skipped by an older pregenerated map.
    boolean hasDatapackEmc =
        !resourceManager
            .listResources("emc", id -> id.getPath().endsWith(".json"))
            .isEmpty();
    boolean hasCustomEmc =
        CustomEMCParser.currentEntries != null
            && !CustomEMCParser.currentEntries.entries().isEmpty();
    boolean usePregenerated = MappingConfig.usePregenerated() && !hasDatapackEmc && !hasCustomEmc;
    Path pregeneratedEmcFile = ProjectEConfig.CONFIG_DIR.resolve("pregenerated_emc.json");
    Optional<Object2LongMap<ItemInfo>> readPregeneratedValues =
        PregeneratedEMC.read(registryAccess, pregeneratedEmcFile, usePregenerated);
    if (readPregeneratedValues.isPresent()) {
      int values = updateEmcValues(readPregeneratedValues.get());
      PECore.debugLog("Loaded {} values from pregenerated EMC File", values);
    } else {
      SimpleGraphMapper.setLogFoundExploits(MappingConfig.logExploits());
      PECore.debugLog("Starting to collect Mappings...");
      List<Exception> failures = new ArrayList<>();

      for (IEMCMapper<NormalizedSimpleStack, Long> emcMapper : mappers) {
        if (MappingConfig.isEnabled(emcMapper)) {
          DumpToFileCollector.currentGroupName = emcMapper.getName();

          try {
            emcMapper.addMappings(
                mappingCollector, serverResources, registryAccess, resourceManager);
            PECore.debugLog("Collected Mappings from " + emcMapper.getClass().getName());
          } catch (Exception e) {
            failures.add(e);
            PECore.LOGGER.error(
                LogUtils.FATAL_MARKER,
                "Exception during Mapping Collection from Mapper {}. PLEASE REPORT THIS! EMC VALUES"
                    + " MIGHT BE INCONSISTENT!",
                emcMapper.getClass().getName(),
                e);
          }
        }
      }

      if (!failures.isEmpty()) {
        IllegalStateException failure =
            new IllegalStateException(
                "EMC mapping failed; retaining the previous complete price table");
        failures.forEach(failure::addSuppressed);
        throw failure;
      }

      DumpToFileCollector.currentGroupName = "NSSHelper";
      PECore.debugLog("Mapping Collection finished");
      mappingCollector.finishCollection(registryAccess);
      PECore.debugLog("Starting to generate Values:");
      Object2LongMap<NormalizedSimpleStack> graphMapperValues = valueGenerator.generateValues();
      PECore.debugLog("Generated Values...");
      updateEmcValues(filterEMCMap(graphMapperValues));
      PECore.debugLog("Filtered Values...");
      if (usePregenerated && emc != null) {
        PregeneratedEMC.write(registryAccess, pregeneratedEmcFile, emc);
        PECore.debugLog("Wrote Pregen-file!");
      }
    }

    fireEmcRemapEvent();
  }

  private static void fireEmcRemapEvent() {
    FuelMapper.loadMap();
    loadIndex++;
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        try {
          IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
          if (knowledge != null) {
            if (knowledge instanceof KnowledgeImpl impl && impl.pruneStaleKnowledge()) {
              knowledge.sync(player);
            } else if (player.containerMenu instanceof TransmutationContainer) {
              PECore.packetHandler().updateTransmutationTargets(player);
            }
          }
        } catch (RuntimeException exception) {
          PECore.LOGGER.error(
              "Could not refresh knowledge for {} after EMC remapping",
              player.getName().getString(),
              exception);
        }
      }
    }

    try {
      NeoForge.EVENT_BUS.post(new EMCRemapEvent());
    } catch (RuntimeException exception) {
      PECore.LOGGER.error(
          "An EMC remap listener failed; the complete price table is retained", exception);
    }
  }

  public static int getLoadIndex() {
    return loadIndex;
  }

  private static Object2LongMap<ItemInfo> filterEMCMap(Object2LongMap<NormalizedSimpleStack> map) {
    Object2LongMap<ItemInfo> resultMap = new Object2LongOpenHashMap<>(map.size());
    Iterator<Entry<NormalizedSimpleStack>> iterator = Object2LongMaps.fastIterator(map);

    while (iterator.hasNext()) {
      Entry<NormalizedSimpleStack> entry = iterator.next();
      if (entry.getKey() instanceof NSSItem nssItem) {
        ItemInfo info = ItemInfo.fromNSS(nssItem);
        if (info != null) {
          resultMap.put(info, entry.getLongValue());
        }
      }
    }

    return resultMap;
  }

  public static int getEmcMapSize() {
    Object2LongMap<ItemInfo> values = emc;
    return values == null ? 0 : values.size();
  }

  public static boolean isAvailable() {
    return emc != null;
  }

  public static boolean hasEmcValue(@NotNull ItemInfo info) {
    Object2LongMap<ItemInfo> values = emc;
    return values != null && values.containsKey(info);
  }

  public static @Range(from = 0L, to = Long.MAX_VALUE) long getStoredEmcValue(
      @NotNull ItemInfo info) {
    Object2LongMap<ItemInfo> values = emc;
    return values == null ? 0L : values.getLong(info);
  }

  public static synchronized void clearEmcMap() {
    emc = null;
    DataComponentManager.updateCachedValues(null);
  }

  public static Set<ItemInfo> getMappedItems() {
    Object2LongMap<ItemInfo> values = emc;
    return values == null ? new HashSet<>() : new HashSet<>(values.keySet());
  }

  @Internal
  public static synchronized int updateEmcValues(Object2LongMap<ItemInfo> data) {
    Object2LongOpenHashMap<ItemInfo> copy = new Object2LongOpenHashMap<>(data);
    copy.defaultReturnValue(0L);
    if (copy.values().longStream().anyMatch(value -> value < 0L)) {
      throw new IllegalArgumentException("An EMC price table cannot contain negative values");
    }
    Object2LongMap<ItemInfo> previous = emc;
    Object2LongMap<ItemInfo> replacement = Object2LongMaps.unmodifiable(copy);
    emc = replacement;
    try {
      DataComponentManager.updateCachedValues(replacement::getLong);
      // Publish processor cache updates to readers together with the completed snapshot.
      emc = replacement;
    } catch (RuntimeException exception) {
      emc = previous;
      try {
        DataComponentManager.updateCachedValues(previous == null ? null : previous::getLong);
        emc = previous;
      } catch (RuntimeException restoreFailure) {
        exception.addSuppressed(restoreFailure);
      }
      throw exception;
    }
    return copy.size();
  }

  public static SyncEmcPKT createPacketData() {
    Object2LongMap<ItemInfo> values = emc;
    return new SyncEmcPKT(values == null ? Object2LongMaps.emptyMap() : values);
  }

  @Internal
  public static void restoreEmcValues(Object2LongMap<ItemInfo> data) {
    updateEmcValues(data);
    fireEmcRemapEvent();
  }
}
