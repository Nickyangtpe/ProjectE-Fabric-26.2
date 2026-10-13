package moze_intel.projecte.gameObjs.registries;

import java.util.function.Function;
import java.util.function.ToIntFunction;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.EnumCollectorTier;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.EnumRelayTier;
import moze_intel.projecte.gameObjs.blocks.AlchemicalChest;
import moze_intel.projecte.gameObjs.blocks.Collector;
import moze_intel.projecte.gameObjs.blocks.Condenser;
import moze_intel.projecte.gameObjs.blocks.CondenserMK2;
import moze_intel.projecte.gameObjs.blocks.InterdictionTorchEntityBlock;
import moze_intel.projecte.gameObjs.blocks.MatterBlock;
import moze_intel.projecte.gameObjs.blocks.MatterFurnace;
import moze_intel.projecte.gameObjs.blocks.Pedestal;
import moze_intel.projecte.gameObjs.blocks.ProjectETNT;
import moze_intel.projecte.gameObjs.blocks.Relay;
import moze_intel.projecte.gameObjs.blocks.TransmutationStone;
import moze_intel.projecte.gameObjs.entity.EntityNovaCataclysmPrimed;
import moze_intel.projecte.gameObjs.entity.EntityNovaCatalystPrimed;
import moze_intel.projecte.gameObjs.registration.impl.BlockDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.BlockRegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class PEBlocks {
  public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister("projecte");
  public static final BlockRegistryObject<AlchemicalChest, BlockItem> ALCHEMICAL_CHEST =
      BLOCKS.register(
          "alchemical_chest",
          () ->
              new AlchemicalChest(
                  blockProperties("alchemical_chest")
                      .mapColor(MapColor.STONE)
                      .instrument(NoteBlockInstrument.BASEDRUM)
                      .requiresCorrectToolForDrops()
                      .strength(10.0F, 3600000.0F)));
  public static final BlockRegistryObject<Block, BlockItem> ALCHEMICAL_COAL =
      registerFuelBlock("alchemical_coal_block", MapColor.COLOR_RED);
  public static final BlockRegistryObject<Block, BlockItem> MOBIUS_FUEL =
      registerFuelBlock("mobius_fuel_block", MapColor.COLOR_RED);
  public static final BlockRegistryObject<Block, BlockItem> AETERNALIS_FUEL =
      registerFuelBlock("aeternalis_fuel_block", MapColor.COLOR_LIGHT_GRAY);
  public static final BlockRegistryObject<Collector, BlockItem> COLLECTOR =
      registerCollector("collector_mk1", EnumCollectorTier.MK1, state -> 7);
  public static final BlockRegistryObject<Collector, BlockItem> COLLECTOR_MK2 =
      registerCollector("collector_mk2", EnumCollectorTier.MK2, state -> 11);
  public static final BlockRegistryObject<Collector, BlockItem> COLLECTOR_MK3 =
      registerCollector("collector_mk3", EnumCollectorTier.MK3, state -> 15);
  public static final BlockRegistryObject<Condenser, BlockItem> CONDENSER =
      registerCondenser(
          "condenser_mk1",
          Condenser::new,
          block -> new BlockItem(block, blockItemProperties("condenser_mk1")));
  public static final BlockRegistryObject<CondenserMK2, BlockItem> CONDENSER_MK2 =
      registerCondenser(
          "condenser_mk2",
          CondenserMK2::new,
          block -> new BlockItem(block, blockItemProperties("condenser_mk2").fireResistant()));
  public static final BlockRegistryObject<Pedestal, BlockItem> DARK_MATTER_PEDESTAL =
      BLOCKS.register(
          "dm_pedestal",
          () ->
              new Pedestal(
                  blockProperties("dm_pedestal")
                      .mapColor(MapColor.COLOR_BLACK)
                      .instrument(NoteBlockInstrument.BASEDRUM)
                      .requiresCorrectToolForDrops()
                      .strength(1000000.0F, 3000000.0F)
                      .lightLevel(state -> 12)),
          block -> new BlockItem(block, blockItemProperties("dm_pedestal").fireResistant()));
  public static final BlockRegistryObject<MatterFurnace, BlockItem> DARK_MATTER_FURNACE =
      registerFurnace("dm_furnace", EnumMatterType.DARK_MATTER, 1000000.0F, 3000000.0F);
  public static final BlockRegistryObject<MatterFurnace, BlockItem> RED_MATTER_FURNACE =
      registerFurnace("rm_furnace", EnumMatterType.RED_MATTER, 2000000.0F, 6000000.0F);
  public static final BlockRegistryObject<MatterBlock, BlockItem> DARK_MATTER =
      registerMatterBlock("dark_matter_block", EnumMatterType.DARK_MATTER, 1000000.0F, 3000000.0F);
  public static final BlockRegistryObject<MatterBlock, BlockItem> RED_MATTER =
      registerMatterBlock("red_matter_block", EnumMatterType.RED_MATTER, 2000000.0F, 6000000.0F);
  public static final BlockRegistryObject.WallOrFloorBlockRegistryObject<
          InterdictionTorchEntityBlock.InterdictionTorch,
          InterdictionTorchEntityBlock.InterdictionTorchWall,
          StandingAndWallBlockItem>
      INTERDICTION_TORCH =
          BLOCKS.registerWallOrFloorItem(
              "interdiction_torch",
              InterdictionTorchEntityBlock.InterdictionTorch::new,
              InterdictionTorchEntityBlock.InterdictionTorchWall::new,
              blockProperties("interdiction_torch")
                  .pushReaction(PushReaction.DESTROY)
                  .noCollision()
                  .instabreak()
                  .strength(0.0F)
                  .lightLevel(state -> 14)
                  .randomTicks());
  public static final BlockRegistryObject<ProjectETNT, BlockItem> NOVA_CATALYST =
      registerExplosive("nova_catalyst", EntityNovaCatalystPrimed::new);
  public static final BlockRegistryObject<ProjectETNT, BlockItem> NOVA_CATACLYSM =
      registerExplosive("nova_cataclysm", EntityNovaCataclysmPrimed::new);
  public static final BlockRegistryObject<TransmutationStone, BlockItem> TRANSMUTATION_TABLE =
      BLOCKS.register(
          "transmutation_table",
          () ->
              new TransmutationStone(
                  blockProperties("transmutation_table")
                      .mapColor(MapColor.STONE)
                      .instrument(NoteBlockInstrument.BASEDRUM)
                      .requiresCorrectToolForDrops()
                      .strength(10.0F, 30.0F)));
  public static final BlockRegistryObject<Relay, BlockItem> RELAY =
      registerRelay("relay_mk1", EnumRelayTier.MK1, state -> 7);
  public static final BlockRegistryObject<Relay, BlockItem> RELAY_MK2 =
      registerRelay("relay_mk2", EnumRelayTier.MK2, state -> 11);
  public static final BlockRegistryObject<Relay, BlockItem> RELAY_MK3 =
      registerRelay("relay_mk3", EnumRelayTier.MK3, state -> 15);

  private PEBlocks() {}

  private static Properties blockProperties(String name) {
    ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, PECore.rl(name));
    return Properties.of().setId(key);
  }

  private static net.minecraft.world.item.Item.Properties blockItemProperties(String name) {
    ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PECore.rl(name));
    return new net.minecraft.world.item.Item.Properties().useBlockDescriptionPrefix().setId(key);
  }

  private static BlockRegistryObject<Block, BlockItem> registerFuelBlock(
      String name, MapColor mapColor) {
    return BLOCKS.register(
        name,
        () ->
            new Block(
                blockProperties(name)
                    .mapColor(mapColor)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F, 1.5F)));
  }

  private static BlockRegistryObject<Collector, BlockItem> registerCollector(
      String name, EnumCollectorTier collectorTier, ToIntFunction<BlockState> lightLevel) {
    return BLOCKS.register(
        name,
        () ->
            new Collector(
                collectorTier,
                blockProperties(name)
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.PLING)
                    .sound(SoundType.GLASS)
                    .requiresCorrectToolForDrops()
                    .strength(0.3F, 0.9F)
                    .lightLevel(lightLevel)));
  }

  private static <CONDENSER extends Condenser>
      BlockRegistryObject<CONDENSER, BlockItem> registerCondenser(
          String name,
          Function<Properties, CONDENSER> condenserFunction,
          Function<CONDENSER, BlockItem> itemCreator) {
    return BLOCKS.register(
        name,
        () ->
            condenserFunction.apply(
                blockProperties(name)
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(10.0F, 3600000.0F)),
        itemCreator);
  }

  private static BlockRegistryObject<Relay, BlockItem> registerRelay(
      String name, EnumRelayTier relayTier, ToIntFunction<BlockState> lightLevel) {
    return BLOCKS.register(
        name,
        () ->
            new Relay(
                relayTier,
                blockProperties(name)
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(10.0F, 30.0F)
                    .lightLevel(lightLevel)));
  }

  private static BlockRegistryObject<ProjectETNT, BlockItem> registerExplosive(
      String name, ProjectETNT.TNTEntityCreator tntEntityCreator) {
    return BLOCKS.register(
        name,
        () ->
            new ProjectETNT(
                blockProperties(name)
                    .mapColor(MapColor.FIRE)
                    .strength(0.0F)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .ignitedByLava()
                    .isRedstoneConductor((state, getter, pos) -> false),
                tntEntityCreator));
  }

  private static BlockRegistryObject<MatterFurnace, BlockItem> registerFurnace(
      String name, EnumMatterType matterType, float hardness, float resistance) {
    return BLOCKS.register(
        name,
        () ->
            new MatterFurnace(
                blockProperties(name)
                    .requiresCorrectToolForDrops()
                    .strength(hardness, resistance)
                    .mapColor(matterType.getMapColor())
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .lightLevel(state -> 14),
                matterType),
        block -> new BlockItem(block, blockItemProperties(name).fireResistant()));
  }

  private static BlockRegistryObject<MatterBlock, BlockItem> registerMatterBlock(
      String name, EnumMatterType matterType, float hardness, float resistance) {
    return BLOCKS.register(
        name,
        () ->
            new MatterBlock(
                blockProperties(name)
                    .requiresCorrectToolForDrops()
                    .strength(hardness, resistance)
                    .mapColor(matterType.getMapColor())
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .lightLevel(state -> 14),
                matterType),
        block -> new BlockItem(block, blockItemProperties(name).fireResistant()));
  }
}
