package moze_intel.projecte.integration.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PEProbeInfoProvider implements IProbeInfoProvider {
  static final PEProbeInfoProvider INSTANCE = new PEProbeInfoProvider();

  private PEProbeInfoProvider() {}

  public void addProbeInfo(
      ProbeMode mode,
      IProbeInfo probeInfo,
      Player player,
      Level level,
      BlockState blockState,
      IProbeHitData data) {
    if (ProjectEConfig.server.misc.lookingAtDisplay.get()) {
      long value = IEMCProxy.INSTANCE.getValue(blockState.getBlock());
      if (value > 0L) {
        probeInfo.mcText(EMCHelper.getEmcTextComponent(value, 1));
      }
    }
  }

  public Identifier getID() {
    return PECore.rl("emc");
  }
}
