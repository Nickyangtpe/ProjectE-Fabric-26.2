package moze_intel.projecte.emc.generator;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import moze_intel.projecte.api.mapper.generator.IValueGenerator;
import moze_intel.projecte.utils.MathUtils;
import org.apache.commons.math3.fraction.BigFraction;

public class BigFractionToLongGenerator<T> implements IValueGenerator<T, Long> {
  private final IValueGenerator<T, BigFraction> inner;

  public BigFractionToLongGenerator(IValueGenerator<T, BigFraction> inner) {
    this.inner = inner;
  }

  public Object2LongMap<T> generateValues() {
    Map<T, BigFraction> innerResult = this.inner.generateValues();
    Object2LongMap<T> myResult = new Object2LongOpenHashMap<>();

    for (Entry<T, BigFraction> entry : innerResult.entrySet()) {
      T key = entry.getKey();
      BigFraction value = entry.getValue();
      BigInteger numerator = value.getNumerator();
      if (numerator.signum() == 1) {
        BigInteger bigInt = numerator.divide(value.getDenominator());
        if (bigInt.signum() == 1) {
          int compare = bigInt.compareTo(MathUtils.MAX_LONG);
          if (compare == 0) {
            myResult.put(key, Long.MAX_VALUE);
          } else if (compare < 0) {
            myResult.put(key, bigInt.longValue());
          }
        }
      }
    }

    return myResult;
  }
}
