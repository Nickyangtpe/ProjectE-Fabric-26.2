package moze_intel.projecte.gameObjs;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public enum EnumRelayTier implements StringRepresentable {
  MK1("relay_mk1", 64L, 100000L),
  MK2("relay_mk2", 192L, 1000000L),
  MK3("relay_mk3", 640L, 10000000L);

  private final String name;
  private final long chargeRate;
  private final long storage;

  EnumRelayTier(
      String name,
      @Range(from = 0L, to = Long.MAX_VALUE) long chargeRate,
      @Range(from = 1L, to = Long.MAX_VALUE) long storage) {
    this.name = name;
    this.chargeRate = chargeRate;
    this.storage = storage;
  }

  @NotNull
  public String getSerializedName() {
    return this.name;
  }

  public @Range(from = 0L, to = Long.MAX_VALUE) long getChargeRate() {
    return this.chargeRate;
  }

  public @Range(from = 1L, to = Long.MAX_VALUE) long getStorage() {
    return this.storage;
  }

  @Override
  public String toString() {
    return this.getSerializedName();
  }
}
