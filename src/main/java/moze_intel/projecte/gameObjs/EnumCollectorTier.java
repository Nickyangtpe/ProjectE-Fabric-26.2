package moze_intel.projecte.gameObjs;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public enum EnumCollectorTier implements StringRepresentable {
  MK1("collector_mk1", 4L, 10000L),
  MK2("collector_mk2", 12L, 30000L),
  MK3("collector_mk3", 40L, 60000L);

  private final String name;
  private final long genRate;
  private final long storage;

  EnumCollectorTier(
      String name,
      @Range(from = 0L, to = Long.MAX_VALUE) long genRate,
      @Range(from = 1L, to = Long.MAX_VALUE) long storage) {
    this.name = name;
    this.genRate = genRate;
    this.storage = storage;
  }

  @NotNull
  public String getSerializedName() {
    return this.name;
  }

  public @Range(from = 0L, to = Long.MAX_VALUE) long getGenRate() {
    return this.genRate;
  }

  public @Range(from = 1L, to = Long.MAX_VALUE) long getStorage() {
    return this.storage;
  }

  @Override
  public String toString() {
    return this.getSerializedName();
  }
}
