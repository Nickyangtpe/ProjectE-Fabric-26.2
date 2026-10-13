package moze_intel.projecte.compat.common;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ItemAbility {
  private static final ConcurrentMap<String, ItemAbility> INSTANCES = new ConcurrentHashMap<>();
  private final String id;

  private ItemAbility(String id) {
    this.id = Objects.requireNonNull(id, "id");
  }

  public static ItemAbility get(String id) {
    String checkedId = Objects.requireNonNull(id, "id");
    return INSTANCES.computeIfAbsent(checkedId, ItemAbility::new);
  }

  public String id() {
    return this.id;
  }

  @Override
  public String toString() {
    return this.id;
  }
}
