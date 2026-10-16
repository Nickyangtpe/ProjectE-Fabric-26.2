package moze_intel.projecte.utils.text;

import moze_intel.projecte.compat.common.TranslatableEnum;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public interface IHasTranslationKey {
  String getTranslationKey();

  interface IHasEnumNameTranslationKey extends IHasTranslationKey, TranslatableEnum {
    @NotNull
    @Override
    default Component getTranslatedName() {
      return TextComponentUtil.translate(this.getTranslationKey());
    }
  }
}
