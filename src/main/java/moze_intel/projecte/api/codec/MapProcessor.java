package moze_intel.projecte.api.codec;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface MapProcessor<KEY, VALUE> {
  MapProcessor<?, ?> PUT_IF_ABSENT = Map::putIfAbsent;

  static <KEY, VALUE> MapProcessor<KEY, VALUE> putIfAbsent() {
    return (MapProcessor<KEY, VALUE>) PUT_IF_ABSENT;
  }

  @Nullable
  VALUE addElement(Map<KEY, VALUE> var1, KEY var2, VALUE var3);
}
