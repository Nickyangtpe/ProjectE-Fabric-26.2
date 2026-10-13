package moze_intel.projecte.integration.jade;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class PEJadePlugin implements IWailaPlugin {
  public void registerClient(IWailaClientRegistration registration) {
    registration.registerBlockComponent(JadeDataProvider.INSTANCE, Block.class);
  }
}
