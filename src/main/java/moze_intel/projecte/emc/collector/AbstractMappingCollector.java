package moze_intel.projecte.emc.collector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import moze_intel.projecte.api.mapper.collector.IExtendedMappingCollector;
import moze_intel.projecte.utils.Constants;
import net.minecraft.core.HolderLookup.Provider;

public abstract class AbstractMappingCollector<
        T, V extends Comparable<V>, A extends IValueArithmetic<?>>
    implements IExtendedMappingCollector<T, V, A> {
  private final A defaultArithmetic;

  AbstractMappingCollector(A defaultArithmetic) {
    this.defaultArithmetic = defaultArithmetic;
  }

  @Override
  public void addConversion(
      int outnumber, T output, Iterable<T> ingredients, A arithmeticForConversion) {
    this.addConversion(
        outnumber, output, this.listToMapOfCounts(ingredients), arithmeticForConversion);
  }

  private Object2IntMap<T> listToMapOfCounts(Iterable<T> iterable) {
    Object2IntMap<T> map = new Object2IntOpenHashMap<>();

    for (T ingredient : iterable) {
      map.mergeInt(ingredient, 1, Constants.INT_SUM);
    }

    return map;
  }

  @Override
  public void setValueFromConversion(int outnumber, T output, Iterable<T> ingredients) {
    this.setValueFromConversion(outnumber, output, this.listToMapOfCounts(ingredients));
  }

  @Override
  public A getArithmetic() {
    return this.defaultArithmetic;
  }

  @Override
  public void finishCollection(Provider registries) {}
}
