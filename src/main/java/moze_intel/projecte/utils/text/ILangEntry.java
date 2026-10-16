package moze_intel.projecte.utils.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public interface ILangEntry extends IHasTranslationKey {
  default MutableComponent translate(Object... args) {
    return TextComponentUtil.smartTranslate(this.getTranslationKey(), args);
  }

  default MutableComponent translate() {
    return TextComponentUtil.translate(this.getTranslationKey());
  }

  default MutableComponent translateColored(ChatFormatting color, Object... args) {
    return TextComponentUtil.build(color, this.translate(args));
  }

  default MutableComponent translateColored(ChatFormatting color) {
    return TextComponentUtil.build(color, this.translate());
  }
}
