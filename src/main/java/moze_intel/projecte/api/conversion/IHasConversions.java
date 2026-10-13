package moze_intel.projecte.api.conversion;

import java.util.List;

public interface IHasConversions {
  List<CustomConversion> conversions();

  default void addConversion(CustomConversion conversion) {
    this.conversions().add(conversion);
  }
}
