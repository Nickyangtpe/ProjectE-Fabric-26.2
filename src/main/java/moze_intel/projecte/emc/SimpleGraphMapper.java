package moze_intel.projecte.emc;

import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import moze_intel.projecte.api.mapper.generator.IValueGenerator;
import moze_intel.projecte.emc.collector.MappingCollector;
import org.jetbrains.annotations.Nullable;

public class SimpleGraphMapper<T, V extends Comparable<V>, A extends IValueArithmetic<V>>
    extends MappingCollector<T, V, A> implements IValueGenerator<T, V> {
  private static final boolean OVERWRITE_FIXED_VALUES = false;
  private final V ZERO;
  private static boolean logFoundExploits = true;

  public SimpleGraphMapper(A arithmetic) {
    super(arithmetic);
    this.ZERO = arithmetic.getZero();
  }

  static void setLogFoundExploits(boolean log) {
    logFoundExploits = log;
  }

  private void addReason(@Nullable Map<T, Object> reasonForChange, T key, Object reason) {
    if (reasonForChange != null) {
      reasonForChange.put(key, reason);
    }
  }

  private boolean updateMapWithMinimum(Map<T, V> m, T key, V value) {
    V stored = m.get(key);
    if (stored != null && stored.compareTo(value) <= 0) {
      return false;
    }

    m.put(key, value);
    return true;
  }

  private boolean canOverride(T something, V value) {
    V valueBeforeInherit = this.fixValueBeforeInherit.get(something);
    return valueBeforeInherit == null || valueBeforeInherit.compareTo(value) == 0;
  }

  private boolean canOverrideZero(T something) {
    V valueBeforeInherit = this.fixValueBeforeInherit.get(something);
    return valueBeforeInherit == null || this.arithmetic.isZero(valueBeforeInherit);
  }

  @Override
  public Map<T, V> generateValues() {
    Map<T, V> values = new HashMap<>();
    Map<T, V> changedValues = new HashMap<>(this.fixValueBeforeInherit);
    Map<T, Object> reasonForChange = null;
    if (isDebugGraphmapper()) {
      reasonForChange = new HashMap<>(changedValues.size());

      for (Entry<T, V> entry : this.fixValueBeforeInherit.entrySet()) {
        reasonForChange.put(entry.getKey(), "fixValueBefore");
      }
    }

    while (changedValues != null && !changedValues.isEmpty()) {
      while (changedValues != null && !changedValues.isEmpty()) {
        Map<T, V> nextChangedValues = null;
        debugPrintln("Loop");

        for (Entry<T, V> entry : changedValues.entrySet()) {
          T key = entry.getKey();
          V value = entry.getValue();
          if (this.canOverride(key, value) && this.updateMapWithMinimum(values, key, value)) {
            if (reasonForChange != null) {
              debugFormat(
                  "Set Value for {} to {} because {}", key, value, reasonForChange.get(key));
            }

            Set<MappingCollector<T, V, A>.Conversion> usesFor = this.usedIn.get(key);
            if (usesFor != null) {
              for (MappingCollector<T, V, A>.Conversion conversion : usesFor) {
                MappingCollector<T, V, A>.Conversion oldConversion =
                    this.overwriteConversion.get(conversion.output);
                if (oldConversion == null || oldConversion == conversion) {
                  V ingredientValue = this.valueForConversion(values, conversion);
                  V resultValueConversion =
                      conversion.arithmeticForConversion.div(ingredientValue, conversion.outnumber);
                  if (this.arithmetic.isGreaterThanZero(resultValueConversion)
                      || conversion.arithmeticForConversion.isFree(resultValueConversion)) {
                    V storedValue = values.get(conversion.output);
                    if (storedValue == null || storedValue.compareTo(resultValueConversion) > 0) {
                      if (nextChangedValues == null) {
                        nextChangedValues = new HashMap<>();
                      }

                      if (this.updateMapWithMinimum(
                          nextChangedValues, conversion.output, resultValueConversion)) {
                        this.addReason(reasonForChange, conversion.output, key);
                      }
                    }
                  }
                }
              }
            }
          }
        }

        changedValues = nextChangedValues;
      }

      for (Entry<T, Set<MappingCollector<T, V, A>.Conversion>> entry :
          this.conversionsFor.entrySet()) {
        T key = entry.getKey();
        V minConversionValue = null;
        V resultValueActual = values.get(key);
        if (resultValueActual != null && this.arithmetic.isZero(resultValueActual)) {
          resultValueActual = null;
        }

        for (MappingCollector<T, V, A>.Conversion conversion : entry.getValue()) {
          V ingredientValue = this.valueForConversion(values, conversion);
          V resultValueConversion =
              conversion.arithmeticForConversion.div(ingredientValue, conversion.outnumber);
          if ((this.arithmetic.isGreaterThanZero(resultValueConversion)
                  || conversion.arithmeticForConversion.isFree(resultValueConversion))
              && (minConversionValue == null
                  || minConversionValue.compareTo(resultValueConversion) > 0)) {
            minConversionValue = resultValueConversion;
          }

          if (this.arithmetic.isGreaterThanZero(ingredientValue)
              && this.isLessThan(resultValueConversion, resultValueActual)) {
            MappingCollector<T, V, A>.Conversion oldConversion =
                this.overwriteConversion.get(conversion.output);
            if (oldConversion != null && oldConversion != conversion) {
              if (logFoundExploits) {
                PECore.LOGGER.warn(
                    "EMC Exploit: \"{}\" ingredient cost: {} value of result: {}"
                        + " setValueFromConversion: {}",
                    new Object[] {
                      conversion,
                      ingredientValue,
                      this.valueOrZero(resultValueActual),
                      oldConversion
                    });
              }
            } else if (this.canOverrideZero(key)) {
              if (isDebugGraphmapper()) {
                debugFormat(
                    "Setting {} to 0 because result ({}) > cost ({}): {}",
                    key,
                    this.valueOrZero(resultValueActual),
                    ingredientValue,
                    conversion);
                this.addReason(reasonForChange, conversion.output, "exploit recipe");
              }

              if (changedValues == null) {
                changedValues = new HashMap<>();
              }

              changedValues.put(conversion.output, this.ZERO);
            } else if (logFoundExploits) {
              PECore.LOGGER.warn(
                  "EMC Exploit: ingredients ({}) cost {} but output value is {}",
                  new Object[] {conversion, ingredientValue, this.valueOrZero(resultValueActual)});
            }
          }
        }

        if (minConversionValue == null
            && resultValueActual != null
            && this.arithmetic.isGreaterThanZero(resultValueActual)
            && this.canOverrideZero(key)) {
          debugFormat(
              "Removing Value for {} because it does not have any nonzero-conversions anymore.",
              key);
          if (changedValues == null) {
            changedValues = new HashMap<>();
          }

          changedValues.put(key, this.ZERO);
          this.addReason(reasonForChange, key, "all conversions dead");
        }
      }
    }

    debugPrintln("");
    values.putAll(this.fixValueAfterInherit);
    values.entrySet().removeIf(something -> this.arithmetic.isFree(something.getValue()));
    return values;
  }

  private V valueOrZero(@Nullable V value) {
    return value == null ? this.ZERO : value;
  }

  private boolean isLessThan(V resultValueConversion, @Nullable V resultValueActual) {
    return resultValueActual == null
        ? this.arithmetic.isLessThanZero(resultValueConversion)
        : resultValueConversion.compareTo(resultValueActual) < 0;
  }

  private V valueForConversion(Map<T, V> values, MappingCollector<T, V, A>.Conversion conversion) {
    try {
      return this.valueForConversionUnsafe(values, conversion);
    } catch (ArithmeticException e) {
      PECore.LOGGER.warn(
          "Could not calculate value for {}: {}", conversion.toString(), e.toString());
      return this.ZERO;
    } catch (Exception e) {
      PECore.LOGGER.warn(
          "Could not calculate value for {}: {}", new Object[] {conversion.toString(), e, e});
      return this.ZERO;
    }
  }

  private V valueForConversionUnsafe(
      Map<T, V> values, MappingCollector<T, V, A>.Conversion conversion) {
    V value = conversion.value;
    boolean allIngredientsAreFree = true;
    boolean hasPositiveIngredientValues = false;
    Iterator<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<T>> iterator =
        Object2IntMaps.fastIterator(conversion.ingredientsWithAmount);

    while (iterator.hasNext()) {
      it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<T> entry = iterator.next();
      V storedValue = values.get(entry.getKey());
      if (storedValue == null) {
        return this.ZERO;
      }

      int amount = entry.getIntValue();
      V ingredientValue = conversion.arithmeticForConversion.mul(amount, storedValue);
      if (this.arithmetic.isZero(ingredientValue)) {
        return this.ZERO;
      }

      V newValue = conversion.arithmeticForConversion.add(value, ingredientValue);
      if (value != newValue || conversion.arithmeticForConversion.isZero(ingredientValue)) {
        value = newValue;
        if (this.arithmetic.isGreaterThanZero(ingredientValue) && amount > 0) {
          hasPositiveIngredientValues = true;
        }

        allIngredientsAreFree = false;
      }
    }

    return !allIngredientsAreFree
            && (!hasPositiveIngredientValues || !this.arithmetic.isLessThanEqualZero(value))
        ? value
        : conversion.arithmeticForConversion.getFree();
  }
}
