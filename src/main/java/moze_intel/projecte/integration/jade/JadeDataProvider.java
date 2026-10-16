package moze_intel.projecte.integration.jade;

import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class JadeDataProvider implements IBlockComponentProvider {
  static final JadeDataProvider INSTANCE = new JadeDataProvider();

  private JadeDataProvider() {}

  public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    if (ProjectEConfig.server.misc.lookingAtDisplay.get()) {
      long value = IEMCProxy.INSTANCE.getValue(accessor.getBlock());
      if (value > 0L) {
        tooltip.add(EMCHelper.getEmcTextComponent(value, 1));
      }
    }
  }

  public Identifier getUid() {
    return PEJadeConstants.EMC_PROVIDER;
  }
}
