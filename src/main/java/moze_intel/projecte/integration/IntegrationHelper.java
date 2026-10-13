package moze_intel.projecte.integration;

import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.integration.trinkets.TrinketsCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IntegrationHelper {
  public static final String TRINKETS_MODID = "trinkets";
  public static final String TRINKETS_UPDATED_MODID = "trinkets_updated";
  public static final String CURIO_MODID = "trinkets";
  public static final String EMI_MODID = "emi";
  public static final String TOP_MODID = "theoneprobe";
  private static boolean initialized;

  private IntegrationHelper() {}

  public static synchronized void initialize() {
    if (!initialized) {
      initialized = true;
      if (isTrinketsLoaded()) {
        PECore.LOGGER.info(
            "Detected Trinkets Updated; ProjectE accessory slots, item ticks, and attributes are"
                + " enabled");
      }

      if (isEmiLoaded()) {
        PECore.LOGGER.info(
            "Detected EMI; ProjectE's EMI integration remains deferred until the 26.2 EMI API is"
                + " available");
      }

      if (isTheOneProbeLoaded()) {
        PECore.LOGGER.info(
            "Detected The One Probe; ProjectE's EMC block-tooltip provider is available");
      }
    }
  }

  public static boolean isTrinketsLoaded() {
    FabricLoader loader = FabricLoader.getInstance();
    return loader.isModLoaded("trinkets") || loader.isModLoaded("trinkets_updated");
  }

  public static boolean isCuriosLoaded() {
    return isTrinketsLoaded();
  }

  public static boolean isEmiLoaded() {
    return FabricLoader.getInstance().isModLoaded("emi");
  }

  public static boolean isTheOneProbeLoaded() {
    return FabricLoader.getInstance().isModLoaded("theoneprobe");
  }

  @Nullable
  public static IItemHandler getAccessoryInventory(@NotNull Player player) {
    return !isTrinketsLoaded() ? null : TrinketsCompat.getAllAccessoryInventories(player);
  }

  public static void registerCuriosCapability(
      @NotNull RegisterCapabilitiesEvent event, @NotNull Item item) {
    registerTrinketItem(item);
  }

  public static void registerTrinketItem(@NotNull Item item) {
    if (isTrinketsLoaded()) {
      TrinketsCompat.registerItem(item);
    }
  }
}
