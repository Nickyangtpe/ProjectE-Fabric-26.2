package moze_intel.projecte.api.data;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.api.conversion.CustomConversion;
import moze_intel.projecte.api.nss.NSSTag;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConversionBuilder<PARENT> implements ConversionBuilderNSSHelper<PARENT> {
  private final PARENT parent;
  private final Object2IntSortedMap<NormalizedSimpleStack> ingredients =
      new Object2IntLinkedOpenHashMap();
  private final NormalizedSimpleStack output;
  private final int outputAmount;
  private boolean propagateTags;

  ConversionBuilder(PARENT parent, NormalizedSimpleStack output, int outputAmount) {
    this.parent = parent;
    this.output = output;
    this.outputAmount = outputAmount;
  }

  CustomConversion build() {
    return new CustomConversion(
        this.outputAmount, this.output, this.ingredients, this.propagateTags);
  }

  @Override
  public String toString() {
    return this.output + " " + this.outputAmount;
  }

  public PARENT end() {
    if (this.ingredients.isEmpty()) {
      throw new RuntimeException("Conversion does not contain any ingredients.");
    } else {
      return this.parent;
    }
  }

  public ConversionBuilder<PARENT> propagateTags() {
    if (this.propagateTags) {
      throw new RuntimeException("Propagate tags has already been set, remove unnecessary call.");
    } else if (this.output instanceof NSSTag nssTag && !nssTag.representsTag()) {
      throw new RuntimeException(
          "Propagate tags can only be enabled for conversion outputs that are tags.");
    } else {
      this.propagateTags = true;
      return this;
    }
  }

  @Override
  public ConversionBuilder<PARENT> ingredient(NormalizedSimpleStack input, int amount) {
    if (this.ingredients.containsKey(input)) {
      throw new RuntimeException(
          "Conversion already contains ingredient '" + input + "', merge identical ingredients.");
    }

    if (amount == 0) {
      throw new RuntimeException(
          "Conversion for empty ingredient '" + input + "' should be removed.");
    }

    this.ingredients.put(input, amount);
    return this;
  }
}
