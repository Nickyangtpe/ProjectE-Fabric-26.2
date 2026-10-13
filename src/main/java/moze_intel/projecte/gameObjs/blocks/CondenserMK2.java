package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.gameObjs.block_entities.CondenserMK2BlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.Nullable;

public class CondenserMK2 extends Condenser {
  public CondenserMK2(Properties props) {
    super(props);
  }

  @Nullable
  @Override
  public BlockEntityTypeRegistryObject<CondenserMK2BlockEntity> getType() {
    return PEBlockEntityTypes.CONDENSER_MK2;
  }
}
