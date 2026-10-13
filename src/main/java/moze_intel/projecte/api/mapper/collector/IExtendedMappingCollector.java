package moze_intel.projecte.api.mapper.collector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;

public interface IExtendedMappingCollector<
        T, V extends Comparable<V>, A extends IValueArithmetic<?>>
    extends IMappingCollector<T, V> {
  void addConversion(int var1, T var2, Object2IntMap<T> var3, A var4);

  @Override
  default void addConversion(int outnumber, T output, Iterable<T> ingredients) {
    this.addConversion(outnumber, output, ingredients, this.getArithmetic());
  }

  void addConversion(int var1, T var2, Iterable<T> var3, A var4);

  @Override
  default void addConversion(int outnumber, T output, Object2IntMap<T> ingredientsWithAmount) {
    this.addConversion(outnumber, output, ingredientsWithAmount, this.getArithmetic());
  }

  A getArithmetic();
}
