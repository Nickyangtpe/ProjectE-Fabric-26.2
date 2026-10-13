package moze_intel.projecte.api.nss;

import java.util.function.Consumer;
import org.apache.logging.log4j.util.TriConsumer;

public interface NSSTag extends NormalizedSimpleStack {
  boolean representsTag();

  void forEachElement(Consumer<NormalizedSimpleStack> var1);

  <CONTEXT, DATA> void forEachElement(
      CONTEXT var1, DATA var2, TriConsumer<CONTEXT, NormalizedSimpleStack, DATA> var3);

  @Override
  default void forSelfAndEachElement(Consumer<NormalizedSimpleStack> consumer) {
    NormalizedSimpleStack.super.forSelfAndEachElement(consumer);
    this.forEachElement(consumer);
  }

  @Override
  default <CONTEXT, DATA> void forSelfAndEachElement(
      CONTEXT context, DATA data, TriConsumer<CONTEXT, NormalizedSimpleStack, DATA> consumer) {
    NormalizedSimpleStack.super.forSelfAndEachElement(context, data, consumer);
    this.forEachElement(context, data, consumer);
  }
}
