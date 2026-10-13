package moze_intel.projecte.emc.collector;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import org.jetbrains.annotations.NotNull;

public abstract class MappingCollector<T, V extends Comparable<V>, A extends IValueArithmetic<V>>
    extends AbstractMappingCollector<T, V, A> {
  private static final boolean DEBUG_GRAPHMAPPER = false;
  private final Function<T, Set<MappingCollector<T, V, A>.Conversion>> CREATE_CONVERSIONS =
      t -> new LinkedHashSet<>();
  protected final A arithmetic;
  protected final Map<@NotNull T, MappingCollector.@NotNull Conversion> overwriteConversion =
      new HashMap<>();
  protected final Map<@NotNull T, @NotNull Set<MappingCollector<T, V, A>.Conversion>>
      conversionsFor = new HashMap<>();
  protected final Map<@NotNull T, @NotNull Set<MappingCollector<T, V, A>.Conversion>> usedIn =
      new HashMap<>();
  protected final Map<@NotNull T, @NotNull V> fixValueBeforeInherit = new HashMap<>();
  protected final Map<@NotNull T, @NotNull V> fixValueAfterInherit = new HashMap<>();

  protected MappingCollector(A arithmetic) {
    super(arithmetic);
    this.arithmetic = arithmetic;
  }

  protected static boolean isDebugGraphmapper() {
    return false;
  }

  protected static void debugFormat(String format, Object... args) {
    if (isDebugGraphmapper()) {
      PECore.debugLog(format, args);
    }
  }

  protected static void debugPrintln(String s) {
    debugFormat(s);
  }

  private Set<MappingCollector<T, V, A>.Conversion> getConversionsFor(@NotNull T something) {
    return this.conversionsFor.computeIfAbsent(something, this.CREATE_CONVERSIONS);
  }

  protected void removeUseFor(
      @NotNull T something, @NotNull MappingCollector<T, V, A>.Conversion conversion) {
    Set<MappingCollector<T, V, A>.Conversion> conversions = this.usedIn.get(something);
    if (conversions != null) {
      conversions.remove(conversion);
    }
  }

  protected Set<MappingCollector<T, V, A>.Conversion> getUsesFor(@NotNull T something) {
    return this.usedIn.computeIfAbsent(something, this.CREATE_CONVERSIONS);
  }

  private void addConversionToIngredientUsages(MappingCollector<T, V, A>.Conversion conversion) {
    ObjectIterator var2 = conversion.ingredientsWithAmount.keySet().iterator();

    while (var2.hasNext()) {
      T ingredient = (T) var2.next();
      this.getUsesFor(ingredient).add(conversion);
    }
  }

  @Override
  public void addConversion(
      int outnumber, T output, Object2IntMap<T> ingredientsWithAmount, A arithmeticForConversion) {
    if (output == null || ingredientsWithAmount.containsKey(null)) {
      PECore.debugLog(
          "Ignoring Recipe because of invalid ingredient or output: {} -> {}x{}",
          ingredientsWithAmount,
          outnumber,
          output);
    } else {
      if (outnumber <= 0) {
        throw new IllegalArgumentException("outnumber has to be > 0!");
      }

      MappingCollector<T, V, A>.Conversion conversion =
          new Conversion(
              output,
              outnumber,
              ingredientsWithAmount,
              arithmeticForConversion,
              this.arithmetic.getZero());
      if (this.getConversionsFor(output).add(conversion)) {
        this.addConversionToIngredientUsages(conversion);
      }
    }
  }

  @Override
  public void setValueBefore(T something, V value) {
    if (something != null && value != null) {
      V valueBeforeInherit = this.fixValueBeforeInherit.get(something);
      if (valueBeforeInherit != null) {
        PECore.debugLog(
            "Overwriting fixValueBeforeInherit for {} from: {} to {}",
            something,
            valueBeforeInherit,
            value);
      }

      this.fixValueBeforeInherit.put(something, value);
      this.fixValueAfterInherit.remove(something);
    }
  }

  @Override
  public void setValueAfter(T something, V value) {
    if (something != null && value != null) {
      V valueAfterInherit = this.fixValueAfterInherit.get(something);
      if (valueAfterInherit != null) {
        PECore.debugLog(
            "Overwriting fixValueAfterInherit for {} from: {} to {}",
            something,
            valueAfterInherit,
            value);
      }

      this.fixValueAfterInherit.put(something, value);
    }
  }

  @Override
  public void setValueFromConversion(
      int outnumber, T something, Object2IntMap<T> ingredientsWithAmount) {
    if (something == null || ingredientsWithAmount.containsKey(null)) {
      PECore.debugLog(
          "Ignoring setValueFromConversion because of invalid ingredient or output: {} -> {}x{}",
          ingredientsWithAmount,
          outnumber,
          something);
    } else {
      if (outnumber <= 0) {
        throw new IllegalArgumentException("outnumber has to be > 0!");
      }

      MappingCollector<T, V, A>.Conversion conversion =
          new Conversion(something, outnumber, ingredientsWithAmount, this.arithmetic);
      MappingCollector<T, V, A>.Conversion oldConversion = this.overwriteConversion.get(something);
      if (oldConversion != null) {
        PECore.debugLog("Overwriting setValueFromConversion {} with {}", oldConversion, conversion);
        ObjectIterator var6 = oldConversion.ingredientsWithAmount.keySet().iterator();

        while (var6.hasNext()) {
          T ingredient = (T) var6.next();
          this.removeUseFor(ingredient, oldConversion);
        }
      }

      this.addConversionToIngredientUsages(conversion);
      this.overwriteConversion.put(something, conversion);
    }
  }

  protected class Conversion {
    public final T output;
    public final int outnumber;
    public final V value;
    public final Object2IntMap<T> ingredientsWithAmount;
    public final A arithmeticForConversion;
    private final int hash;

    Conversion(
        T output,
        int outnumber,
        Object2IntMap<T> ingredientsWithAmount,
        A arithmeticForConversion) {
      this(
          output,
          outnumber,
          ingredientsWithAmount,
          arithmeticForConversion,
          MappingCollector.this.arithmetic.getZero());
    }

    Conversion(
        T output,
        int outnumber,
        Object2IntMap<T> ingredientsWithAmount,
        A arithmeticForConversion,
        V value) {
      this.output = output;
      this.outnumber = outnumber;
      if (ingredientsWithAmount != null && !ingredientsWithAmount.isEmpty()) {
        Object2IntMap<T> filteredMap = new Object2IntOpenHashMap(ingredientsWithAmount.size());
        Iterator<Entry<T>> iterator = Object2IntMaps.fastIterator(ingredientsWithAmount);

        while (iterator.hasNext()) {
          Entry<T> ingredient = iterator.next();
          int amount = ingredient.getIntValue();
          if (amount != 0) {
            filteredMap.put(ingredient.getKey(), amount);
          }
        }

        this.ingredientsWithAmount = filteredMap;
      } else {
        this.ingredientsWithAmount = Object2IntMaps.emptyMap();
      }

      this.arithmeticForConversion = arithmeticForConversion;
      this.value = value;
      this.hash = Objects.hash(this.output, this.value, this.ingredientsWithAmount);
    }

    @Override
    public String toString() {
      return this.value
          + " + "
          + this.ingredientsToString()
          + " => "
          + this.outnumber
          + "*"
          + this.output;
    }

    private String ingredientsToString() {
      if (this.ingredientsWithAmount.isEmpty()) {
        return "nothing";
      }

      StringBuilder ingredients = new StringBuilder();
      boolean addSeparator = false;
      Iterator<Entry<T>> iterator = Object2IntMaps.fastIterator(this.ingredientsWithAmount);

      while (iterator.hasNext()) {
        Entry<T> ingredient = iterator.next();
        if (addSeparator) {
          ingredients.append(" + ");
        } else {
          addSeparator = true;
        }

        ingredients.append(ingredient.getIntValue()).append('*').append(ingredient.getKey());
      }

      return ingredients.toString();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MappingCollector<?, ?, ?>.Conversion other
          && Objects.equals(this.output, other.output)
          && Objects.equals(this.value, other.value)
          && Objects.equals(this.ingredientsWithAmount, other.ingredientsWithAmount);
    }

    @Override
    public int hashCode() {
      return this.hash;
    }
  }
}
