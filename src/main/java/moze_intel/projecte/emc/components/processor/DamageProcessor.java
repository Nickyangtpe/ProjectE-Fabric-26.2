package moze_intel.projecte.emc.components.processor;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@DataComponentProcessor(priority = Integer.MAX_VALUE)
public class DamageProcessor implements IDataComponentProcessor {
  @DataComponentProcessor.Instance
  public static final DamageProcessor INSTANCE = new DamageProcessor();

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_DAMAGE.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_DAMAGE.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_DAMAGE.tooltip();
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info, @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC)
      throws ArithmeticException {
    ItemStack fakeStack = info.createStack();
    if (fakeStack.isDamaged()) {
      int maxDamage = fakeStack.getMaxDamage();
      int remainingDurability = maxDamage - fakeStack.getDamageValue();
      if (remainingDurability == 0) {
        return 0L;
      }

      if (remainingDurability == 1) {
        currentEMC /= maxDamage;
      } else if (remainingDurability > 1) {
        currentEMC = Math.multiplyExact(currentEMC, remainingDurability) / maxDamage;
      }
    }

    return currentEMC;
  }
}
