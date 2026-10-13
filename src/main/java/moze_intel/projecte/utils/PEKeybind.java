package moze_intel.projecte.utils;

import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.function.IntFunction;
import moze_intel.projecte.PECore;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Util;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;

public enum PEKeybind implements IHasTranslationKey.IHasEnumNameTranslationKey {
  HELMET_TOGGLE,
  BOOTS_TOGGLE,
  CHARGE,
  EXTRA_FUNCTION,
  FIRE_PROJECTILE,
  MODE,
  TRANSMUTATION_TABLET;

  public static final IntFunction<PEKeybind> BY_ID =
      ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
  public static final StreamCodec<ByteBuf, PEKeybind> STREAM_CODEC =
      ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
  private final String translationKey =
      Util.makeDescriptionId("key", PECore.rl(this.name().toLowerCase(Locale.ROOT)));

  @Override
  public String getTranslationKey() {
    return this.translationKey;
  }
}
