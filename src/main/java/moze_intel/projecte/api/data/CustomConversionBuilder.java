package moze_intel.projecte.api.data;

import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.api.conversion.CustomConversionFile;
import moze_intel.projecte.api.conversion.FixedValues;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomConversionBuilder extends BaseFileBuilder<CustomConversionBuilder>
    implements CustomConversionBuilderNSSHelper {
  private final Map<String, ConversionGroupBuilder> groups = new LinkedHashMap<>();
  private final Object2LongSortedMap<NormalizedSimpleStack> fixedValueBefore =
      new Object2LongLinkedOpenHashMap();
  private final Object2LongSortedMap<NormalizedSimpleStack> fixedValueAfter =
      new Object2LongLinkedOpenHashMap();
  private final List<ConversionBuilder<?>> fixedValueConversions = new ArrayList<>();
  private boolean replace;

  CustomConversionBuilder() {
    super("Custom Conversion");
  }

  CustomConversionFile build() {
    return new CustomConversionFile(
        this.replace,
        this.comment,
        this.groups.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> ((ConversionGroupBuilder) entry.getValue()).build(),
                    (a, b) -> {
                      throw new IllegalStateException("No duplicate keys");
                    },
                    LinkedHashMap::new)),
        new FixedValues(
            this.fixedValueBefore,
            this.fixedValueAfter,
            this.fixedValueConversions.stream().map(ConversionBuilder::build).toList()));
  }

  public CustomConversionBuilder replace() {
    if (this.replace) {
      throw new RuntimeException("Replace has already been set, remove unnecessary call.");
    }

    this.replace = true;
    return this;
  }

  public ConversionGroupBuilder group(String groupName) {
    Objects.requireNonNull(groupName, "Group name cannot be null.");
    if (groupName.isEmpty()) {
      throw new RuntimeException("Group with name cannot be empty.");
    }

    if (this.groups.containsKey(groupName)) {
      throw new RuntimeException("Group with name '" + groupName + "' already exists.");
    }

    ConversionGroupBuilder builder = new ConversionGroupBuilder(this);
    this.groups.put(groupName, builder);
    return builder;
  }

  @Override
  public CustomConversionBuilder before(NormalizedSimpleStack stack, long emc) {
    return this.fixedValue(stack, emc, this.fixedValueBefore, "before");
  }

  @Override
  public CustomConversionBuilder before(NormalizedSimpleStack stack) {
    return this.fixedValue(stack, Long.MIN_VALUE, this.fixedValueBefore, "before");
  }

  @Override
  public CustomConversionBuilder after(NormalizedSimpleStack stack, long emc) {
    return this.fixedValue(stack, emc, this.fixedValueAfter, "after");
  }

  @Override
  public CustomConversionBuilder after(NormalizedSimpleStack stack) {
    return this.fixedValue(stack, Long.MIN_VALUE, this.fixedValueAfter, "after");
  }

  private CustomConversionBuilder fixedValue(
      NormalizedSimpleStack stack,
      long emc,
      Object2LongMap<NormalizedSimpleStack> fixedValues,
      String type) {
    Objects.requireNonNull(stack, "Normalized Simple Stack cannot be null.");
    if (emc < 1L && emc != Long.MIN_VALUE) {
      throw new IllegalArgumentException("EMC value must be at least one.");
    }

    if (fixedValues.containsKey(stack)) {
      throw new RuntimeException("Fixed value " + type + " already set for '" + stack + "'.");
    }

    fixedValues.put(stack, emc);
    return this;
  }

  @Override
  public ConversionBuilder<CustomConversionBuilder> conversion(
      NormalizedSimpleStack output, int amount) {
    if (amount < 1) {
      throw new IllegalArgumentException(
          "Output amount for fixed value conversions must be at least one.");
    }

    ConversionBuilder<CustomConversionBuilder> builder =
        new ConversionBuilder<>(this, output, amount);
    this.fixedValueConversions.add(builder);
    return builder;
  }
}
