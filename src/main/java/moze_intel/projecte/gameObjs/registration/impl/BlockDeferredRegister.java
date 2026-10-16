package moze_intel.projecte.gameObjs.registration.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.compat.registries.DeferredHolder;
import moze_intel.projecte.gameObjs.registration.DoubleDeferredRegister;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.storage.loot.LootTable;

public class BlockDeferredRegister extends DoubleDeferredRegister<Block, Item> {
  private final String modid;

  public BlockDeferredRegister(String modid) {
    super(Registries.BLOCK, new ItemDeferredRegister(modid), modid);
    this.modid = modid;
  }

  public BlockRegistryObject<Block, BlockItem> register(String name, Properties properties) {
    return this.register(name, () -> new Block(properties.setId(this.blockKey(name))));
  }

  public <BLOCK extends Block> BlockRegistryObject<BLOCK, BlockItem> register(
      String name, Supplier<? extends BLOCK> blockSupplier) {
    return this.register(
        name,
        blockSupplier,
        block ->
            new BlockItem(
                block,
                new net.minecraft.world.item.Item.Properties()
                    .useBlockDescriptionPrefix()
                    .setId(this.itemKey(name))));
  }

  public <BLOCK extends Block, WALL_BLOCK extends Block>
      BlockRegistryObject.WallOrFloorBlockRegistryObject<
              BLOCK, WALL_BLOCK, StandingAndWallBlockItem>
          registerWallOrFloorItem(
              String name,
              Function<Properties, BLOCK> blockSupplier,
              Function<Properties, WALL_BLOCK> wallBlockSupplier,
              Properties baseProperties) {
    DeferredHolder<Block, BLOCK> primaryObject =
        this.primaryRegister.register(
            name, () -> blockSupplier.apply(baseProperties.setId(this.blockKey(name))));
    DeferredHolder<Block, WALL_BLOCK> wallObject =
        this.primaryRegister.register(
            "wall_" + name,
            () -> {
              ResourceKey<LootTable> baseLootTable =
                  ResourceKey.create(
                      Registries.LOOT_TABLE,
                      primaryObject.getKey().identifier().withPrefix("blocks/"));
              Properties wallProperties =
                  Properties.ofFullCopy((BlockBehaviour) primaryObject.get())
                      .setId(this.blockKey("wall_" + name))
                      .overrideLootTable(Optional.of(baseLootTable));
              return wallBlockSupplier.apply(wallProperties);
            });
    return new BlockRegistryObject.WallOrFloorBlockRegistryObject<>(
        primaryObject,
        wallObject,
        this.secondaryRegister.register(
            name,
            () ->
                new StandingAndWallBlockItem(
                    primaryObject.get(),
                    wallObject.get(),
                    Direction.DOWN,
                    new net.minecraft.world.item.Item.Properties()
                        .useBlockDescriptionPrefix()
                        .setId(this.itemKey(name)))));
  }

  private ResourceKey<Block> blockKey(String name) {
    return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(this.modid, name));
  }

  private ResourceKey<Item> itemKey(String name) {
    return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(this.modid, name));
  }

  public <BLOCK extends Block, ITEM extends BlockItem> BlockRegistryObject<BLOCK, ITEM> register(
      String name, Supplier<? extends BLOCK> blockSupplier, Function<BLOCK, ITEM> itemCreator) {
    return this.register(name, blockSupplier, itemCreator, BlockRegistryObject::new);
  }
}
