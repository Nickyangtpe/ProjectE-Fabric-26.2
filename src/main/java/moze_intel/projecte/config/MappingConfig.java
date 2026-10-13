package moze_intel.projecte.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.config.value.CachedBooleanValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MappingConfig extends BasePEConfig {
  private static MappingConfig INSTANCE;
  public final CachedBooleanValue dumpToFile;
  public final CachedBooleanValue usePregenerated;
  public final CachedBooleanValue logExploits;
  private final ModConfigSpec configSpec;
  private final Map<String, BooleanSupplier> mappersEnabledConfig;
  private final Map<String, MappingConfig.ProcessorConfig> processorConfigs;

  public static void setup(
      @NotNull List<IEMCMapper<NormalizedSimpleStack, Long>> mappers,
      @NotNull List<IDataComponentProcessor> processors) {
    if (INSTANCE == null) {
      ProjectEConfig.registerConfig(
          PECore.MOD_CONTAINER, INSTANCE = new MappingConfig(mappers, processors));
    }
  }

  private MappingConfig(
      @NotNull List<IEMCMapper<NormalizedSimpleStack, Long>> mappers,
      @NotNull List<IDataComponentProcessor> processors) {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    this.dumpToFile =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.MAPPING_DUMP_TO_FILE
                .applyToBuilder(builder)
                .define("dumpToFile", false));
    this.usePregenerated =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.MAPPING_PREGENERATED
                .applyToBuilder(builder)
                .define("usePregenerated", false));
    this.logExploits =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.MAPPING_LOG_EXPLOITS
                .applyToBuilder(builder)
                .define("logFoundExploits", true));
    PEConfigTranslations.MAPPING_MAPPERS.applyToBuilder(builder).push("mappers");
    this.mappersEnabledConfig = new HashMap<>(mappers.size());

    for (IEMCMapper<NormalizedSimpleStack, Long> mapper : mappers) {
      builder
          .comment(mapper.getDescription())
          .translation(mapper.getTranslationKey())
          .push(mapper.getConfigPath());
      this.mappersEnabledConfig.put(
          mapper.getName(),
          CachedBooleanValue.wrap(
              this,
              PEConfigTranslations.MAPPING_MAPPER_ENABLED
                  .applyToBuilder(builder)
                  .define("enabled", mapper.isAvailable())));
      mapper.addConfigOptions(builder);
      builder.pop();
    }

    builder.pop();
    PEConfigTranslations.MAPPING_PROCESSORS.applyToBuilder(builder).push("processors");
    this.processorConfigs = new HashMap<>(processors.size());

    for (IDataComponentProcessor processor : processors) {
      this.processorConfigs.put(
          processor.getName(), new MappingConfig.ProcessorConfig(this, builder, processor));
    }

    builder.pop();
    this.configSpec = builder.build();
  }

  public static boolean dumpToFile() {
    return INSTANCE != null && INSTANCE.dumpToFile.get();
  }

  public static boolean usePregenerated() {
    return INSTANCE != null && INSTANCE.usePregenerated.get();
  }

  public static boolean logExploits() {
    return INSTANCE == null || INSTANCE.logExploits.get();
  }

  public static boolean isEnabled(IEMCMapper<NormalizedSimpleStack, Long> mapper) {
    if (INSTANCE == null) {
      return mapper.isAvailable();
    } else {
      String name = mapper.getName();
      BooleanSupplier isEnabled = INSTANCE.mappersEnabledConfig.get(name);
      if (isEnabled == null) {
        PECore.LOGGER.warn("Mapper Config: '{}' is missing from the config.", name);
        return mapper.isAvailable();
      } else {
        return isEnabled.getAsBoolean();
      }
    }
  }

  public static boolean isEnabled(IDataComponentProcessor processor) {
    if (INSTANCE == null) {
      return processor.isAvailable();
    } else {
      String name = processor.getName();
      MappingConfig.ProcessorConfig processorConfig = INSTANCE.processorConfigs.get(name);
      if (processorConfig == null) {
        PECore.LOGGER.warn("Processor Config: '{}' is missing from the config.", name);
        return processor.isAvailable();
      } else {
        return processorConfig.enabled.get();
      }
    }
  }

  public static boolean hasPersistent(IDataComponentProcessor processor) {
    if (INSTANCE == null) {
      return processor.hasPersistentComponents() && processor.usePersistentComponents();
    }

    String name = processor.getName();
    MappingConfig.ProcessorConfig processorConfig = INSTANCE.processorConfigs.get(name);
    if (processorConfig != null) {
      if (processorConfig.persistent == null) {
        if (processor.hasPersistentComponents()) {
          PECore.LOGGER.warn(
              "Processor Config: '{}' has persistent Data Components but is missing the config"
                  + " option.",
              name);
          return processor.usePersistentComponents();
        } else {
          return false;
        }
      } else {
        return processorConfig.persistent.get();
      }
    } else {
      PECore.LOGGER.warn("Persistent processor Config: '{}' is missing from the config.", name);
      return processor.hasPersistentComponents() && processor.usePersistentComponents();
    }
  }

  @Override
  public String getFileName() {
    return "mapping";
  }

  @Override
  public String getTranslation() {
    return "EMC Mapper Config";
  }

  @Override
  public ModConfigSpec getConfigSpec() {
    return this.configSpec;
  }

  @Override
  public ModConfig.Type getConfigType() {
    return ModConfig.Type.SERVER;
  }

  private static class ProcessorConfig {
    private final CachedBooleanValue enabled;
    @Nullable private final CachedBooleanValue persistent;

    private ProcessorConfig(
        IPEConfig config, ModConfigSpec.Builder builder, IDataComponentProcessor processor) {
      builder
          .comment(processor.getDescription())
          .translation(processor.getTranslationKey())
          .push(processor.getConfigPath());
      this.enabled =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.DCP_ENABLED
                  .applyToBuilder(builder)
                  .define("enabled", processor.isAvailable()));
      this.enabled.addInvalidationListener(
          () -> processor.updateCachedValues(this.enabled.get() ? IEMCProxy.INSTANCE : null));
      if (processor.hasPersistentComponents()) {
        this.persistent =
            CachedBooleanValue.wrap(
                config,
                PEConfigTranslations.DCP_PERSISTENT
                    .applyToBuilder(builder)
                    .define("persistent", processor.usePersistentComponents()));
      } else {
        this.persistent = null;
      }

      processor.addConfigOptions(builder);
      builder.pop();
    }
  }
}
