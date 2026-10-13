package moze_intel.projecte.api.mapper.generator;

import java.util.Map;

public interface IValueGenerator<T, V extends Comparable<V>> {
  Map<T, V> generateValues();
}
