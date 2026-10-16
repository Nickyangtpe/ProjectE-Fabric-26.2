package moze_intel.projecte.emc.collector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import moze_intel.projecte.api.mapper.collector.IExtendedMappingCollector;
import org.apache.commons.math3.fraction.BigFraction;

public class LongToBigFractionCollector<T, A extends IValueArithmetic<?>>
    extends AbstractMappingCollector<T, Long, A> {
  private final IExtendedMappingCollector<T, BigFraction, A> inner;

  public LongToBigFractionCollector(IExtendedMappingCollector<T, BigFraction, A> inner) {
    super(inner.getArithmetic());
    this.inner = inner;
  }

  @Override
  public void setValueFromConversion(
      int outnumber, T something, Object2IntMap<T> ingredientsWithAmount) {
    this.inner.setValueFromConversion(outnumber, something, ingredientsWithAmount);
  }

  @Override
  public void addConversion(
      int outnumber, T output, Object2IntMap<T> ingredientsWithAmount, A arithmeticForConversion) {
    this.inner.addConversion(outnumber, output, ingredientsWithAmount, arithmeticForConversion);
  }

  public void setValueBefore(T something, Long value) {
    this.inner.setValueBefore(something, new BigFraction(value));
  }

  public void setValueAfter(T something, Long value) {
    this.inner.setValueAfter(something, new BigFraction(value));
  }
}
