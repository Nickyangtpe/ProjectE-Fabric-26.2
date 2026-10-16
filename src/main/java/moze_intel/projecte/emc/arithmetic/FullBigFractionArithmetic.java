package moze_intel.projecte.emc.arithmetic;

import java.math.BigInteger;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import moze_intel.projecte.utils.MathUtils;
import org.apache.commons.math3.fraction.BigFraction;

public class FullBigFractionArithmetic implements IValueArithmetic<BigFraction> {
  private static final BigInteger FREE_BIG_INT_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigFraction FREE_FRACTION_VALUE = new BigFraction(FREE_BIG_INT_VALUE);

  public BigFraction getZero() {
    return BigFraction.ZERO;
  }

  public boolean isZero(BigFraction value) {
    return value.getNumerator().signum() == 0;
  }

  public boolean isLessThanZero(BigFraction value) {
    return value.getNumerator().signum() == -1;
  }

  public boolean isLessThanEqualZero(BigFraction value) {
    return value.getNumerator().signum() != 1;
  }

  public boolean isGreaterThanEqualZero(BigFraction value) {
    return value.getNumerator().signum() != -1;
  }

  public boolean isGreaterThanZero(BigFraction value) {
    return value.getNumerator().signum() == 1;
  }

  public BigFraction add(BigFraction a, BigFraction b) {
    if (this.isZero(a)) {
      return b;
    } else if (this.isZero(b)) {
      return a;
    } else if (this.isFree(a)) {
      return b;
    } else {
      return this.isFree(b) ? a : a.add(b);
    }
  }

  public BigFraction mul(long a, BigFraction b) {
    return a != 1L && !this.isZeroOrFree(b) ? b.multiply(a) : b;
  }

  public BigFraction div(BigFraction a, long b) {
    if (b == 1L || this.isZeroOrFree(a)) {
      return a;
    }

    if (b == 0L) {
      return this.getZero();
    }

    BigFraction result = a.divide(b);
    return MathUtils.isGreaterThanLong(result.getDenominator()) ? this.getZero() : result;
  }

  public BigFraction getFree() {
    return FREE_FRACTION_VALUE;
  }

  private boolean isZeroOrFree(BigFraction value) {
    if (value == this.getFree()) {
      return true;
    } else {
      int sign = value.getNumerator().signum();
      if (sign == 0) {
        return true;
      } else {
        return sign == 1 ? false : value.getNumerator().equals(FREE_BIG_INT_VALUE);
      }
    }
  }

  public boolean isFree(BigFraction value) {
    if (value == this.getFree()) {
      return true;
    } else {
      return this.isGreaterThanEqualZero(value)
          ? false
          : value.getNumerator().equals(FREE_BIG_INT_VALUE);
    }
  }
}
