package moze_intel.projecte.api;

import moze_intel.projecte.compat.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class PESounds {
  public static final DeferredHolder<SoundEvent, SoundEvent> WIND = get("windmagic");
  public static final DeferredHolder<SoundEvent, SoundEvent> WATER = get("watermagic");
  public static final DeferredHolder<SoundEvent, SoundEvent> POWER = get("power");
  public static final DeferredHolder<SoundEvent, SoundEvent> HEAL = get("heal");
  public static final DeferredHolder<SoundEvent, SoundEvent> DESTRUCT = get("destruct");
  public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE = get("charge");
  public static final DeferredHolder<SoundEvent, SoundEvent> UNCHARGE = get("uncharge");
  public static final DeferredHolder<SoundEvent, SoundEvent> TRANSMUTE = get("transmute");

  private PESounds() {}

  private static DeferredHolder<SoundEvent, SoundEvent> get(String name) {
    return DeferredHolder.create(
        Registries.SOUND_EVENT, Identifier.fromNamespaceAndPath("projecte", name));
  }
}
