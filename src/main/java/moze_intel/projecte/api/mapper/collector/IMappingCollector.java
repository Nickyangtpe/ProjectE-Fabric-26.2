package moze_intel.projecte.api.mapper.collector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.HolderLookup.Provider;

public interface IMappingCollector<T, V extends Comparable<V>> {
  void addConversion(int var1, T var2, Object2IntMap<T> var3);

  void addConversion(int var1, T var2, Iterable<T> var3);

  void setValueBefore(T var1, V var2);

  void setValueAfter(T var1, V var2);

  void setValueFromConversion(int var1, T var2, Iterable<T> var3);

  void setValueFromConversion(int var1, T var2, Object2IntMap<T> var3);

  void finishCollection(Provider var1);
}
