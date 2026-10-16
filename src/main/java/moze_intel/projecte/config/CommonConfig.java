package moze_intel.projecte.config;

import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.config.value.CachedBooleanValue;

public class CommonConfig extends BasePEConfig {
  private final ModConfigSpec configSpec;
  public final CachedBooleanValue debugLogging;
  public final CachedBooleanValue craftableTome;
  public final CachedBooleanValue fullKleinStars;

  CommonConfig() {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    this.debugLogging =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.COMMON_DEBUG_LOGGING
                .applyToBuilder(builder)
                .define("debugLogging", false));
    PEConfigTranslations.COMMON_CRAFTING.applyToBuilder(builder).push("crafting");
    this.craftableTome =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.COMMON_CRAFTING_TOME
                .applyToBuilder(builder)
                .define("craftableTome", false));
    this.fullKleinStars =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.COMMON_CRAFTING_FULL_KLEIN
                .applyToBuilder(builder)
                .define("fullKleinStars", false));
    builder.pop();
    this.configSpec = builder.build();
  }

  @Override
  public String getFileName() {
    return "common";
  }

  @Override
  public String getTranslation() {
    return "Common Config";
  }

  @Override
  public ModConfigSpec getConfigSpec() {
    return this.configSpec;
  }

  @Override
  public ModConfig.Type getConfigType() {
    return ModConfig.Type.COMMON;
  }
}
