package moze_intel.projecte.config;

import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.config.value.CachedBooleanValue;

public class ClientConfig extends BasePEConfig {
  private final ModConfigSpec configSpec;
  public final CachedBooleanValue emcToolTips;
  public final CachedBooleanValue shiftEmcToolTips;
  public final CachedBooleanValue shiftLearnedToolTips;
  public final CachedBooleanValue pedestalToolTips;
  public final CachedBooleanValue statToolTips;
  public final CachedBooleanValue tagToolTips;
  public final CachedBooleanValue pulsatingOverlay;
  public final CachedBooleanValue contextualHud;
  public final CachedBooleanValue contextualHudItemIcon;
  public final CachedBooleanValue contextualHudMode;
  public final CachedBooleanValue contextualHudCharge;
  public final CachedBooleanValue contextualHudKeyHints;

  ClientConfig() {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    this.pulsatingOverlay =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_PHILO_OVERLAY
                .applyToBuilder(builder)
                .define("pulsatingOverlay", false));
    this.contextualHud =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_CONTEXTUAL_HUD
                .applyToBuilder(builder)
                .define("contextualHud", true));
    this.contextualHudItemIcon =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_CONTEXTUAL_HUD_ICON
                .applyToBuilder(builder)
                .define("contextualHudItemIcon", true));
    this.contextualHudMode =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_CONTEXTUAL_HUD_MODE
                .applyToBuilder(builder)
                .define("contextualHudMode", true));
    this.contextualHudCharge =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_CONTEXTUAL_HUD_CHARGE
                .applyToBuilder(builder)
                .define("contextualHudCharge", true));
    this.contextualHudKeyHints =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_CONTEXTUAL_HUD_HINTS
                .applyToBuilder(builder)
                .define("contextualHudKeyHints", true));
    PEConfigTranslations.CLIENT_TOOLTIPS.applyToBuilder(builder).push("tooltips");
    this.emcToolTips =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_TOOLTIPS_EMC.applyToBuilder(builder).define("emc", true));
    this.shiftEmcToolTips =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_TOOLTIPS_EMC_SHIFT
                .applyToBuilder(builder)
                .define("shift_emc", false));
    this.shiftLearnedToolTips =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_TOOLTIPS_LEARNED_SHIFT
                .applyToBuilder(builder)
                .define("shift_learned", true));
    this.pedestalToolTips =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_TOOLTIPS_PEDESTAL
                .applyToBuilder(builder)
                .define("pedestal", true));
    this.statToolTips =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_TOOLTIPS_STATS
                .applyToBuilder(builder)
                .define("statToolTips", true));
    this.tagToolTips =
        CachedBooleanValue.wrap(
            this,
            PEConfigTranslations.CLIENT_TOOLTIPS_TAGS.applyToBuilder(builder).define("tag", false));
    builder.pop();
    this.configSpec = builder.build();
  }

  @Override
  public String getFileName() {
    return "client";
  }

  @Override
  public String getTranslation() {
    return "Client Config";
  }

  @Override
  public ModConfigSpec getConfigSpec() {
    return this.configSpec;
  }

  @Override
  public ModConfig.Type getConfigType() {
    return ModConfig.Type.CLIENT;
  }
}
