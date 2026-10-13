package moze_intel.projecte.config;

import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.compat.fml.config.ModConfig;
import moze_intel.projecte.config.value.CachedBooleanValue;
import moze_intel.projecte.config.value.CachedDoubleValue;
import moze_intel.projecte.config.value.CachedFloatValue;
import moze_intel.projecte.config.value.CachedIntValue;

public final class ServerConfig extends BasePEConfig {
  private final ModConfigSpec configSpec;
  public final ServerConfig.Difficulty difficulty;
  public final ServerConfig.Items items;
  public final ServerConfig.Effects effects;
  public final ServerConfig.Misc misc;
  public final ServerConfig.Cooldown cooldown;

  ServerConfig() {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    this.cooldown = new ServerConfig.Cooldown(this, builder);
    this.difficulty = new ServerConfig.Difficulty(this, builder);
    this.effects = new ServerConfig.Effects(this, builder);
    this.items = new ServerConfig.Items(this, builder);
    this.misc = new ServerConfig.Misc(this, builder);
    this.configSpec = builder.build();
  }

  @Override
  public String getFileName() {
    return "server";
  }

  @Override
  public String getTranslation() {
    return "Server Config";
  }

  @Override
  public ModConfigSpec getConfigSpec() {
    return this.configSpec;
  }

  @Override
  public ModConfig.Type getConfigType() {
    return ModConfig.Type.SERVER;
  }

  public static class Cooldown {
    public final ServerConfig.Cooldown.Pedestal pedestal;
    public final ServerConfig.Cooldown.Player player;

    private Cooldown(IPEConfig config, ModConfigSpec.Builder builder) {
      PEConfigTranslations.SERVER_COOLDOWN.applyToBuilder(builder).push("cooldown");
      this.pedestal = new ServerConfig.Cooldown.Pedestal(config, builder);
      this.player = new ServerConfig.Cooldown.Player(config, builder);
      builder.pop();
    }

    public static class Pedestal {
      public final CachedIntValue archangel;
      public final CachedIntValue body;
      public final CachedIntValue evertide;
      public final CachedIntValue harvest;
      public final CachedIntValue ignition;
      public final CachedIntValue life;
      public final CachedIntValue repair;
      public final CachedIntValue swrg;
      public final CachedIntValue soul;
      public final CachedIntValue volcanite;
      public final CachedIntValue zero;

      private Pedestal(IPEConfig config, ModConfigSpec.Builder builder) {
        PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL.applyToBuilder(builder).push("pedestal");
        this.archangel =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_ARCHANGEL
                    .applyToBuilder(builder)
                    .defineInRange("archangel", 40, -1, Integer.MAX_VALUE));
        this.body =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_BODY_STONE
                    .applyToBuilder(builder)
                    .defineInRange("body", 10, -1, Integer.MAX_VALUE));
        this.evertide =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_EVERTIDE
                    .applyToBuilder(builder)
                    .defineInRange("evertide", 20, -1, Integer.MAX_VALUE));
        this.harvest =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_HARVEST
                    .applyToBuilder(builder)
                    .defineInRange("harvest", 10, -1, Integer.MAX_VALUE));
        this.ignition =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_IGNITION
                    .applyToBuilder(builder)
                    .defineInRange("ignition", 40, -1, Integer.MAX_VALUE));
        this.life =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_LIFE_STONE
                    .applyToBuilder(builder)
                    .defineInRange("life", 5, -1, Integer.MAX_VALUE));
        this.repair =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_REPAIR
                    .applyToBuilder(builder)
                    .defineInRange("repair", 20, -1, Integer.MAX_VALUE));
        this.swrg =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_SWRG
                    .applyToBuilder(builder)
                    .defineInRange("swrg", 70, -1, Integer.MAX_VALUE));
        this.soul =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_SOUL_STONE
                    .applyToBuilder(builder)
                    .defineInRange("soul", 10, -1, Integer.MAX_VALUE));
        this.volcanite =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_VOLCANITE
                    .applyToBuilder(builder)
                    .defineInRange("volcanite", 20, -1, Integer.MAX_VALUE));
        this.zero =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PEDESTAL_ZERO
                    .applyToBuilder(builder)
                    .defineInRange("zero", 40, -1, Integer.MAX_VALUE));
        builder.pop();
      }
    }

    public static class Player {
      public final CachedIntValue projectile;
      public final CachedIntValue gemChest;
      public final CachedIntValue repair;
      public final CachedIntValue heal;
      public final CachedIntValue feed;

      private Player(IPEConfig config, ModConfigSpec.Builder builder) {
        PEConfigTranslations.SERVER_COOLDOWN_PLAYER.applyToBuilder(builder).push("player");
        this.projectile =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PLAYER_PROJECTILE
                    .applyToBuilder(builder)
                    .defineInRange("projectile", 0, -1, Integer.MAX_VALUE));
        this.gemChest =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PLAYER_GEM_CHESTPLATE
                    .applyToBuilder(builder)
                    .defineInRange("gemChest", 0, -1, Integer.MAX_VALUE));
        this.repair =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PLAYER_REPAIR
                    .applyToBuilder(builder)
                    .defineInRange("repair", 20, -1, Integer.MAX_VALUE));
        this.heal =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PLAYER_HEAL
                    .applyToBuilder(builder)
                    .defineInRange("heal", 20, -1, Integer.MAX_VALUE));
        this.feed =
            CachedIntValue.wrap(
                config,
                PEConfigTranslations.SERVER_COOLDOWN_PLAYER_FEED
                    .applyToBuilder(builder)
                    .defineInRange("feed", 20, -1, Integer.MAX_VALUE));
        builder.pop();
      }
    }
  }

  public static class Difficulty {
    public final CachedBooleanValue offensiveAbilities;
    public final CachedFloatValue katarDeathAura;
    public final CachedDoubleValue covalenceLoss;
    public final CachedBooleanValue covalenceLossRounding;

    private Difficulty(IPEConfig config, ModConfigSpec.Builder builder) {
      PEConfigTranslations.SERVER_DIFFICULTY.applyToBuilder(builder).push("difficulty");
      this.offensiveAbilities =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_DIFFICULTY_OFFENSIVE_ABILITIES
                  .applyToBuilder(builder)
                  .define("offensiveAbilities", false));
      this.katarDeathAura =
          CachedFloatValue.wrap(
              config,
              PEConfigTranslations.SERVER_DIFFICULTY_KATAR_DEATH_AURA
                  .applyToBuilder(builder)
                  .defineInRange("katarDeathAura", 1000.0, 0.0, 2.147483647E9));
      this.covalenceLoss =
          CachedDoubleValue.wrap(
              config,
              PEConfigTranslations.SERVER_DIFFICULTY_COVALENCE_LOSS
                  .applyToBuilder(builder)
                  .defineInRange("covalenceLoss", 1.0, 0.1, 1.0));
      this.covalenceLossRounding =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_DIFFICULTY_COVALENCE_LOSS_ROUNDING
                  .applyToBuilder(builder)
                  .define("covalenceLossRounding", true));
      builder.pop();
    }
  }

  public static class Effects {
    public final CachedIntValue timePedBonus;
    public final CachedDoubleValue timePedMobSlowness;
    public final CachedBooleanValue interdictionMode;

    private Effects(IPEConfig config, ModConfigSpec.Builder builder) {
      PEConfigTranslations.SERVER_EFFECTS.applyToBuilder(builder).push("effects");
      this.timePedBonus =
          CachedIntValue.wrap(
              config,
              PEConfigTranslations.SERVER_EFFECTS_TIME_PEDESTAL_BONUS
                  .applyToBuilder(builder)
                  .defineInRange("timePedBonus", 18, 0, 256));
      this.timePedMobSlowness =
          CachedDoubleValue.wrap(
              config,
              PEConfigTranslations.SERVER_EFFECTS_TIME_PEDESTAL_MOB_SLOWNESS
                  .applyToBuilder(builder)
                  .defineInRange("timePedMobSlowness", 0.1, 0.0, 1.0));
      this.interdictionMode =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_EFFECTS_INTERDICTION_MODE
                  .applyToBuilder(builder)
                  .define("interdictionMode", true));
      builder.pop();
    }
  }

  public static class Items {
    public final CachedBooleanValue pickaxeAoeVeinMining;
    public final CachedBooleanValue harvBandIndirect;
    public final CachedBooleanValue disableAllRadiusMining;
    public final CachedBooleanValue enableTimeWatch;
    public final CachedBooleanValue opEvertide;

    private Items(IPEConfig config, ModConfigSpec.Builder builder) {
      PEConfigTranslations.SERVER_ITEMS.applyToBuilder(builder).push("items");
      this.pickaxeAoeVeinMining =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_ITEMS_PICKAXE_AOE_VEIN_MINING
                  .applyToBuilder(builder)
                  .define("pickaxeAoeVeinMining", false));
      this.harvBandIndirect =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_ITEMS_HARVEST_BAND_INDIRECT
                  .applyToBuilder(builder)
                  .define("harvBandIndirect", false));
      this.disableAllRadiusMining =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_ITEMS_DISABLE_ALL_RADIUS_MINING
                  .applyToBuilder(builder)
                  .define("disableAllRadiusMining", false));
      this.enableTimeWatch =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_ITEMS_TIME_WATCH
                  .applyToBuilder(builder)
                  .define("enableTimeWatch", true));
      this.opEvertide =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_ITEMS_OP_EVERTIDE
                  .applyToBuilder(builder)
                  .define("opEvertide", false));
      builder.pop();
    }
  }

  public static class Misc {
    public final CachedBooleanValue unsafeKeyBinds;
    public final CachedBooleanValue lookingAtDisplay;

    private Misc(IPEConfig config, ModConfigSpec.Builder builder) {
      PEConfigTranslations.SERVER_MISC.applyToBuilder(builder).push("misc");
      this.unsafeKeyBinds =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_MISC_UNSAFE_KEY_BINDS
                  .applyToBuilder(builder)
                  .define("unsafeKeyBinds", false));
      this.lookingAtDisplay =
          CachedBooleanValue.wrap(
              config,
              PEConfigTranslations.SERVER_MISC_LOOKING_AT_DISPLAY
                  .applyToBuilder(builder)
                  .define("lookingAtDisplay", true));
      builder.pop();
    }
  }
}
