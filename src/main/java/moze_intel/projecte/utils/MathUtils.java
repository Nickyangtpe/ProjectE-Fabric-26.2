package moze_intel.projecte.utils;

import java.math.BigInteger;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;

public final class MathUtils {
  private static final BigInteger MAX_INTEGER = BigInteger.valueOf(2147483647L);
  public static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

  public static int scaleToRedstone(long currentAmount, long max) {
    if (currentAmount <= 0L) {
      return 0;
    }

    if (currentAmount >= max) {
      return 15;
    }

    double proportion = (double) currentAmount / max;
    return (int) Math.round(proportion * 13.0 + 1.0);
  }

  public static float tickToSec(int ticks, float tickRate) {
    if (tickRate <= 1.0F) {
      throw new IllegalArgumentException("Tick rate below minimum tick rate");
    } else {
      return ticks / tickRate;
    }
  }

  public static Component tickToSecFormatted(int ticks, float tickRate) {
    float result = tickToSec(ticks, tickRate);
    return result == 0.0F ? PELang.EVERY_TICK.translate(result) : PELang.SECONDS.translate(result);
  }

  public static int secToTicks(float secs, float tickRate) {
    return Math.round(secs * tickRate);
  }

  public static int clampToInt(BigInteger bigInt) {
    return bigInt.compareTo(MAX_INTEGER) >= 0 ? Integer.MAX_VALUE : bigInt.intValue();
  }

  public static long clampToLong(BigInteger bigInt) {
    return bigInt.compareTo(MAX_LONG) >= 0 ? Long.MAX_VALUE : bigInt.longValue();
  }

  public static boolean isGreaterThanLong(BigInteger bigInt) {
    return bigInt.compareTo(MAX_LONG) > 0;
  }
}
