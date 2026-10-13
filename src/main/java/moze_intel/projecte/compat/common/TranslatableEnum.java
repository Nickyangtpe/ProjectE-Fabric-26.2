package moze_intel.projecte.compat.common;

import net.minecraft.network.chat.Component;

@FunctionalInterface
public interface TranslatableEnum {
  Component getTranslatedName();
}
