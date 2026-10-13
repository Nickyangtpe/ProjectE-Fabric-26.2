package moze_intel.projecte.emc.components.processor;

import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class MapScaleProcessor implements IDataComponentProcessor {
  @DataComponentProcessor.Instance
  public static final MapScaleProcessor INSTANCE = new MapScaleProcessor();

  private long paperEmc;
  private long lockEmc;

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_MAP_EXTENSION.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_MAP_EXTENSION.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_MAP_EXTENSION.tooltip();
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info, @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC)
      throws ArithmeticException {
    if (!(info.getItem().value() instanceof MapItem)) {
      return currentEMC;
    }

    ItemStack fakeStack = info.createStack();
    MapPostProcessing postProcessing =
        (MapPostProcessing) fakeStack.get(DataComponents.MAP_POST_PROCESSING);
    Level level = tryGetLevel();
    MapItemSavedData data = level == null ? null : MapItem.getSavedData(fakeStack, level);
    boolean locked = postProcessing == MapPostProcessing.LOCK;
    int scale = postProcessing == MapPostProcessing.SCALE ? 1 : 0;
    if (data != null) {
      scale += data.scale;
      if (scale > 4) {
        scale = 4;
      }

      if (!locked) {
        locked = data.locked;
      }
    }

    if (locked) {
      if (this.lockEmc == 0L) {
        return 0L;
      }

      currentEMC = Math.addExact(currentEMC, this.lockEmc);
    }

    if (scale > 0) {
      if (this.paperEmc == 0L) {
        return 0L;
      }

      currentEMC = Math.addExact(currentEMC, Math.multiplyExact(this.paperEmc, scale));
    }

    return currentEMC;
  }

  @Override
  public void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    if (emcLookup == null) {
      this.lockEmc = this.paperEmc = 0L;
    } else {
      this.paperEmc = emcLookup.applyAsLong(ItemInfo.fromItem(Items.PAPER));
      this.lockEmc = emcLookup.applyAsLong(ItemInfo.fromItem(Items.GLASS_PANE));
    }
  }

  @Nullable
  private static Level tryGetLevel() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      return server.overworld();
    } else {
      return FMLEnvironment.dist.isClient() ? MapScaleProcessor.ClientLevelHelper.getLevel() : null;
    }
  }

  private static class ClientLevelHelper {
    @Nullable
    public static Level getLevel() {
      return Minecraft.getInstance().level;
    }
  }
}
