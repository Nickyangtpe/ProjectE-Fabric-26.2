package moze_intel.projecte.emc;

import java.util.Comparator;
import java.util.stream.StreamSupport;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.network.packets.to_client.SyncFuelMapperPKT;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FuelMapper {
  private static volatile HolderSet<Item> FUEL_MAP = HolderSet.empty();

  public static void loadMap() {
    FUEL_MAP =
        HolderSet.direct(
            StreamSupport.<Holder<Item>>stream(
                    BuiltInRegistries.ITEM.getTagOrEmpty(PETags.Items.COLLECTOR_FUEL).spliterator(),
                    false)
                .filter(IEMCProxy.INSTANCE::hasValue)
                .sorted(Comparator.comparingLong(IEMCProxy.INSTANCE::getValue))
                .toList());
  }

  public static void setFuelMap(HolderSet<Item> map) {
    FUEL_MAP = map;
  }

  public static SyncFuelMapperPKT getSyncPacket() {
    return new SyncFuelMapperPKT(FUEL_MAP);
  }

  public static boolean isStackFuel(ItemStack stack) {
    return stack.isEmpty() ? false : FUEL_MAP.contains(stack.typeHolder());
  }

  public static boolean isStackMaxFuel(ItemStack stack) {
    HolderSet<Item> fuels = FUEL_MAP;
    return fuels.size() > 0 && stack.is(fuels.get(fuels.size() - 1));
  }

  public static ItemStack getFuelUpgrade(ItemStack stack) {
    Holder<Item> fuelUpgrade = getFuelUpgrade(stack.typeHolder());
    return fuelUpgrade == null ? ItemStack.EMPTY : new ItemStack(fuelUpgrade);
  }

  @Nullable
  public static Holder<Item> getFuelUpgrade(Holder<Item> holder) {
    if (holder.is(PETags.Items.COLLECTOR_FUEL)) {
      int i = 0;

      for (int elements = FUEL_MAP.size(); i < elements; i++) {
        if (holder.is(FUEL_MAP.get(i))) {
          if (i + 1 == elements) {
            return null;
          }

          return FUEL_MAP.get(i + 1);
        }
      }
    }

    PECore.LOGGER.warn("Tried to upgrade invalid fuel: {}", holder.getRegisteredName());
    return null;
  }

  public static HolderSet<Item> getFuelMap() {
    return FUEL_MAP;
  }
}
