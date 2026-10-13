package moze_intel.projecte.gameObjs.registries;

import com.mojang.serialization.DataResult;
import moze_intel.projecte.components.GemData;
import moze_intel.projecte.gameObjs.items.DiviningRod;
import moze_intel.projecte.gameObjs.items.GemEternalDensity;
import moze_intel.projecte.gameObjs.items.MercurialEye;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import moze_intel.projecte.gameObjs.items.rings.Arcana;
import moze_intel.projecte.gameObjs.items.rings.SWRG;
import moze_intel.projecte.gameObjs.items.rings.TimeWatch;
import moze_intel.projecte.gameObjs.items.tools.PEKatar;
import moze_intel.projecte.gameObjs.items.tools.PEPickaxe;
import moze_intel.projecte.gameObjs.registration.PEDataComponentType;
import moze_intel.projecte.gameObjs.registration.impl.DataComponentTypeDeferredRegister;
import net.minecraft.world.item.component.ItemContainerContents;

public class PEDataComponentTypes {
  public static final DataComponentTypeDeferredRegister DATA_COMPONENT_TYPES =
      new DataComponentTypeDeferredRegister("projecte");
  public static final PEDataComponentType<ItemContainerContents> EYE_INVENTORY =
      DATA_COMPONENT_TYPES.simple(
          "eye_inventory",
          builder ->
              builder
                  .persistent(
                      ItemContainerContents.CODEC.validate(
                          contents ->
                              contents.allItemsCopyStream().limit(3L).count() > 2L
                                  ? DataResult.error(
                                      () -> "The eye cannot have more than two items stored")
                                  : DataResult.success(contents)))
                  .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                  .cacheEncoding());
  public static final PEDataComponentType<Byte> COOLDOWN =
      DATA_COMPONENT_TYPES.registerByte("cooldown", (byte) 0, (byte) 20);
  public static final PEDataComponentType<Integer> CHARGE =
      DATA_COMPONENT_TYPES.registerNonNegativeInt("charge");
  public static final PEDataComponentType<Integer> STORED_EXP =
      DATA_COMPONENT_TYPES.registerNonNegativeInt("stored_exp");
  public static final PEDataComponentType<Long> STORED_EMC =
      DATA_COMPONENT_TYPES.registerNonNegativeLong("stored_emc");
  public static final PEDataComponentType<Double> UNPROCESSED_EMC =
      DATA_COMPONENT_TYPES.registerNonNegativeDouble("unprocessed_emc");
  public static final PEDataComponentType<Boolean> ACTIVE =
      DATA_COMPONENT_TYPES.registerBoolean("active");
  public static final PEDataComponentType<Boolean> STEP_ASSIST =
      DATA_COMPONENT_TYPES.registerBoolean("step_assist");
  public static final PEDataComponentType<Boolean> NIGHT_VISION =
      DATA_COMPONENT_TYPES.registerBoolean("night_vision");
  public static final PEDataComponentType<GemData> GEM_DATA =
      DATA_COMPONENT_TYPES.simple(
          "gem_data",
          builder ->
              builder
                  .persistent(GemData.CODEC)
                  .networkSynchronized(GemData.STREAM_CODEC)
                  .cacheEncoding());
  public static final PEDataComponentType<TimeWatch.TimeWatchMode> TIME_WATCH_MODE =
      DATA_COMPONENT_TYPES.simple(
          "time_watch_mode",
          builder ->
              builder
                  .persistent(TimeWatch.TimeWatchMode.CODEC)
                  .networkSynchronized(TimeWatch.TimeWatchMode.STREAM_CODEC));
  public static final PEDataComponentType<Arcana.ArcanaMode> ARCANA_MODE =
      DATA_COMPONENT_TYPES.simple(
          "arcana_mode",
          builder ->
              builder
                  .persistent(Arcana.ArcanaMode.CODEC)
                  .networkSynchronized(Arcana.ArcanaMode.STREAM_CODEC));
  public static final PEDataComponentType<SWRG.SWRGMode> SWRG_MODE =
      DATA_COMPONENT_TYPES.simple(
          "swrg_mode",
          builder ->
              builder
                  .persistent(SWRG.SWRGMode.CODEC)
                  .networkSynchronized(SWRG.SWRGMode.STREAM_CODEC));
  public static final PEDataComponentType<MercurialEye.MercurialEyeMode> MERCURIAL_EYE_MODE =
      DATA_COMPONENT_TYPES.simple(
          "eye_mode",
          builder ->
              builder
                  .persistent(MercurialEye.MercurialEyeMode.CODEC)
                  .networkSynchronized(MercurialEye.MercurialEyeMode.STREAM_CODEC));
  public static final PEDataComponentType<PhilosophersStone.PhilosophersStoneMode>
      PHILOSOPHERS_STONE_MODE =
          DATA_COMPONENT_TYPES.simple(
              "philosophers_mode",
              builder ->
                  builder
                      .persistent(PhilosophersStone.PhilosophersStoneMode.CODEC)
                      .networkSynchronized(PhilosophersStone.PhilosophersStoneMode.STREAM_CODEC));
  public static final PEDataComponentType<PEKatar.KatarMode> KATAR_MODE =
      DATA_COMPONENT_TYPES.simple(
          "katar_mode",
          builder ->
              builder
                  .persistent(PEKatar.KatarMode.CODEC)
                  .networkSynchronized(PEKatar.KatarMode.STREAM_CODEC));
  public static final PEDataComponentType<PEPickaxe.PickaxeMode> PICKAXE_MODE =
      DATA_COMPONENT_TYPES.simple(
          "pickaxe_mode",
          builder ->
              builder
                  .persistent(PEPickaxe.PickaxeMode.CODEC)
                  .networkSynchronized(PEPickaxe.PickaxeMode.STREAM_CODEC));
  public static final PEDataComponentType<GemEternalDensity.GemMode> GEM_MODE =
      DATA_COMPONENT_TYPES.simple(
          "gem_mode",
          builder ->
              builder
                  .persistent(GemEternalDensity.GemMode.CODEC)
                  .networkSynchronized(GemEternalDensity.GemMode.STREAM_CODEC));
  public static final PEDataComponentType<DiviningRod.DiviningMode> DIVINING_ROD_MODE =
      DATA_COMPONENT_TYPES.simple(
          "divining_mode",
          builder ->
              builder
                  .persistent(DiviningRod.DiviningMode.CODEC)
                  .networkSynchronized(DiviningRod.DiviningMode.STREAM_CODEC));

  private PEDataComponentTypes() {}
}
