package moze_intel.projecte.integration.jade;

import net.minecraft.world.level.block.Block;
import moze_intel.projecte.gameObjs.block_entities.EmcBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class PEJadePlugin implements IWailaPlugin {
  @Override
  public void register(IWailaCommonRegistration registration) {
    registration.registerBlockDataProvider(JadeMachineDataProvider.INSTANCE, EmcBlockEntity.class);
  }

  @Override
  public void registerClient(IWailaClientRegistration registration) {
    registration.registerBlockComponent(JadeDataProvider.INSTANCE, Block.class);
    registration.registerBlockComponent(JadeMachineComponentProvider.INSTANCE, Block.class);
  }
}
