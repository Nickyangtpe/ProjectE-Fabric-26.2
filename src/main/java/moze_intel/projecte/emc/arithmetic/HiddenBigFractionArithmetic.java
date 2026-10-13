package moze_intel.projecte.emc.arithmetic;

import org.apache.commons.math3.fraction.BigFraction;

public class HiddenBigFractionArithmetic extends FullBigFractionArithmetic {
  @Override
  public BigFraction div(BigFraction a, long b) {
    BigFraction result = super.div(a, b);
    return this.isGreaterThanEqualZero(result) && this.isLessThanOne(result)
        ? result
        : new BigFraction(result.getNumerator().divide(result.getDenominator()));
  }

  private boolean isLessThanOne(BigFraction result) {
    return result.getNumerator().signum() != 1
        || result.getNumerator().compareTo(result.getDenominator()) < 0;
  }
}
