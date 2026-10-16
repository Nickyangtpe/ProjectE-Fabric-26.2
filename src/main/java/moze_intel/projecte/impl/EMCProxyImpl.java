package moze_intel.projecte.impl;

import java.util.Objects;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.components.DataComponentManager;
import moze_intel.projecte.utils.EMCHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class EMCProxyImpl implements IEMCProxy {
  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long getValue(@NotNull ItemInfo info) {
    return DataComponentManager.getEmcValue(Objects.requireNonNull(info));
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long getSellValue(@NotNull ItemInfo info) {
    return EMCHelper.getEmcSellValue(this.getValue(info));
  }
}
