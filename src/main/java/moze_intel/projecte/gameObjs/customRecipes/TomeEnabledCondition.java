package moze_intel.projecte.gameObjs.customRecipes;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.compat.conditions.ICondition;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PERecipeConditions;
import org.jetbrains.annotations.NotNull;

public class TomeEnabledCondition implements ICondition {
  public static final TomeEnabledCondition INSTANCE = new TomeEnabledCondition();

  private TomeEnabledCondition() {}

  @Override
  public boolean test() {
    return ProjectEConfig.common.craftableTome.get();
  }

  @NotNull
  @Override
  public MapCodec<? extends ICondition> codec() {
    return PERecipeConditions.TOME_ENABLED.value();
  }
}
