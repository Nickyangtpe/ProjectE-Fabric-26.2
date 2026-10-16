package moze_intel.projecte.integration.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.TheOneProbePlugin;

public final class TOPIntegration implements TheOneProbePlugin {
  public void register(ITheOneProbe theOneProbe) {
    theOneProbe.registerProvider(PEProbeInfoProvider.INSTANCE);
  }
}
