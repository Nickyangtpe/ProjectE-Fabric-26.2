package moze_intel.projecte.utils;

import moze_intel.projecte.PECore;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class TransmutationEMCFormatter {
  private static final int MAX_POSTFIX_INDEX = 17;

  public static Component formatEMC(Number emc) {
    String emcAsString = emc.toString();
    int length = emcAsString.length();
    int splits = (length - 1) / 3;
    if (splits < 4) {
      return TextComponentUtil.getString(EMCHelper.formatEmc(emc));
    }

    int postfixIndex = splits - 4;
    if (postfixIndex > 17) {
      return PELang.EMC_TOO_MUCH.translate();
    }

    int extraDigits = length % 3;
    double value;
    if (extraDigits == 0) {
      value = Double.parseDouble(emcAsString.substring(0, 3) + "." + emcAsString.substring(3, 5));
    } else if (extraDigits == 1) {
      value = Double.parseDouble(emcAsString.charAt(0) + "." + emcAsString.substring(1, 3));
    } else {
      value = Double.parseDouble(emcAsString.substring(0, 2) + "." + emcAsString.substring(2, 4));
    }

    return TextComponentUtil.smartTranslate(
        Util.makeDescriptionId("emc", PECore.rl("postfix." + postfixIndex)),
        EMCHelper.formatEmc(value));
  }
}
