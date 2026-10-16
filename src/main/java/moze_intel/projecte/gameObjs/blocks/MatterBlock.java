package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.gameObjs.IMatterType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class MatterBlock extends Block implements IMatterBlock {
  public final IMatterType matterType;

  public MatterBlock(Properties props, IMatterType type) {
    super(props);
    this.matterType = type;
  }

  @Override
  public IMatterType getMatterType() {
    return this.matterType;
  }
}
