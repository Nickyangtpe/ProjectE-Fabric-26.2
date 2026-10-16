package moze_intel.projecte.api.mapper.arithmetic;

public interface IValueArithmetic<T extends Comparable<T>> {
  default boolean isZero(T value) {
    return this.getZero().equals(value);
  }

  boolean isLessThanZero(T var1);

  boolean isLessThanEqualZero(T var1);

  boolean isGreaterThanZero(T var1);

  boolean isGreaterThanEqualZero(T var1);

  T getZero();

  T add(T var1, T var2);

  T mul(long var1, T var3);

  T div(T var1, long var2);

  T getFree();

  boolean isFree(T var1);
}
