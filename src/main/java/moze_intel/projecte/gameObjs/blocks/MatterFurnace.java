package moze_intel.projecte.gameObjs.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlockTypes;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatterFurnace extends AbstractFurnaceBlock
    implements IMatterBlock, PEEntityBlock<DMFurnaceBlockEntity> {
  private final EnumMatterType matterType;

  public MatterFurnace(Properties props, EnumMatterType type) {
    super(props);
    this.matterType = type;
  }

  @Nullable
  @Override
  public BlockEntityTypeRegistryObject<? extends DMFurnaceBlockEntity> getType() {
    return this.matterType == EnumMatterType.RED_MATTER
        ? PEBlockEntityTypes.RED_MATTER_FURNACE
        : PEBlockEntityTypes.DARK_MATTER_FURNACE;
  }

  @NotNull
  protected MapCodec<MatterFurnace> codec() {
    return PEBlockTypes.MATTER_FURNACE.value();
  }

  protected void openContainer(Level level, @NotNull BlockPos pos, @NotNull Player player) {
    if (!level.isClientSide()) {
      DMFurnaceBlockEntity furnace =
          WorldHelper.getBlockEntity(DMFurnaceBlockEntity.class, level, pos, true);
      if (furnace != null) {
        ContainerTypeDeferredRegister.openMenu(player, furnace, pos);
      }
    }
  }

  @Deprecated
  public int getAnalogOutputSignal(
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Direction direction) {
    return ItemHandlerHelper.calcRedstoneFromInventory(
        WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, pos, state, null, null));
  }

  public EnumMatterType getMatterType() {
    return this.matterType;
  }
}
