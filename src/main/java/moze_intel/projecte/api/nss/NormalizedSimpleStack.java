package moze_intel.projecte.api.nss;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import org.apache.logging.log4j.util.TriConsumer;

public interface NormalizedSimpleStack {
  MapCodec<? extends NormalizedSimpleStack> codec();

  default void forSelfAndEachElement(Consumer<NormalizedSimpleStack> consumer) {
    consumer.accept(this);
  }

  default <CONTEXT, DATA> void forSelfAndEachElement(
      CONTEXT context, DATA data, TriConsumer<CONTEXT, NormalizedSimpleStack, DATA> consumer) {
    consumer.accept(context, this, data);
  }

  @Override
  boolean equals(Object var1);

  @Override
  int hashCode();

  @Override
  String toString();
}
