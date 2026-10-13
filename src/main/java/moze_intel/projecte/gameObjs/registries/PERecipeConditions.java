package moze_intel.projecte.gameObjs.registries;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.compat.conditions.ICondition;
import moze_intel.projecte.compat.registries.NeoForgeRegistries;
import moze_intel.projecte.gameObjs.customRecipes.FullKleinStarsCondition;
import moze_intel.projecte.gameObjs.customRecipes.TomeEnabledCondition;
import moze_intel.projecte.gameObjs.registration.DeferredCodecHolder;
import moze_intel.projecte.gameObjs.registration.DeferredCodecRegister;
import net.minecraft.core.Registry;

public class PERecipeConditions {
  private static final Registry<MapCodec<? extends ICondition>> CONDITION_CODEC_REGISTRY =
      NeoForgeRegistries.CONDITION_CODECS;
  public static final DeferredCodecRegister<ICondition> CONDITION_CODECS =
      new DeferredCodecRegister<>(NeoForgeRegistries.Keys.CONDITION_CODECS, "projecte");
  public static final DeferredCodecHolder<ICondition, TomeEnabledCondition> TOME_ENABLED =
      CONDITION_CODECS.registerUnit("tome_enabled", () -> TomeEnabledCondition.INSTANCE);
  public static final DeferredCodecHolder<ICondition, FullKleinStarsCondition> FULL_KLEIN_STARS =
      CONDITION_CODECS.registerUnit("full_klein_stars", () -> FullKleinStarsCondition.INSTANCE);
}
