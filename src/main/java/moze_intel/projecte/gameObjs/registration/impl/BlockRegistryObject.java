package moze_intel.projecte.gameObjs.registration.impl;

import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.compat.registries.DeferredHolder;
import moze_intel.projecte.gameObjs.registration.DoubleWrappedRegistryObject;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@ParametersAreNonnullByDefault
public class BlockRegistryObject<BLOCK extends Block, ITEM extends Item>
    extends DoubleWrappedRegistryObject<Block, BLOCK, Item, ITEM>
    implements ItemLike, IHasTranslationKey {
  public BlockRegistryObject(
      DeferredHolder<Block, BLOCK> blockRegistryObject,
      DeferredHolder<Item, ITEM> itemRegistryObject) {
    super(blockRegistryObject, itemRegistryObject);
  }

  @NotNull
  public BLOCK getBlock() {
    return this.getPrimary();
  }

  @NotNull
  public BlockState defaultState() {
    return this.getBlock().defaultBlockState();
  }

  @NotNull
  public ITEM asItem() {
    return this.getSecondary();
  }

  @Override
  public String getTranslationKey() {
    return this.getBlock().getDescriptionId();
  }

  @Internal
  Block[] getBlocks() {
    return new Block[] {this.getBlock()};
  }

  public static class WallOrFloorBlockRegistryObject<
          BLOCK extends Block, WALL_BLOCK extends Block, ITEM extends StandingAndWallBlockItem>
      extends BlockRegistryObject<BLOCK, ITEM> {
    @NotNull private final DeferredHolder<Block, WALL_BLOCK> wallRO;

    public WallOrFloorBlockRegistryObject(
        DeferredHolder<Block, BLOCK> blockRegistryObject,
        DeferredHolder<Block, WALL_BLOCK> wallBlockRegistryObject,
        DeferredHolder<Item, ITEM> itemRegistryObject) {
      super(blockRegistryObject, itemRegistryObject);
      this.wallRO = wallBlockRegistryObject;
    }

    @NotNull
    public WALL_BLOCK getWallBlock() {
      return this.wallRO.get();
    }

    public String getWallName() {
      return this.wallRO.getId().getPath();
    }

    @Internal
    @Override
    Block[] getBlocks() {
      return new Block[] {this.getBlock(), this.getWallBlock()};
    }
  }
}
