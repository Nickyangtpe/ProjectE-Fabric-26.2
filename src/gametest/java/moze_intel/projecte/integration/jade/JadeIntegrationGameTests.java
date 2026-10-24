package moze_intel.projecte.integration.jade;

import java.util.List;
import java.util.function.Supplier;
import moze_intel.projecte.gameObjs.block_entities.DMPedestalBlockEntity;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.api.BlockAccessor;
import snownee.jade.impl.BlockAccessorImpl;

public class JadeIntegrationGameTests {
  private static final BlockPos TARGET = new BlockPos(3, 3, 3);

  @GameTest
  public void pedestalDataPreservesItemStatusDescriptionAndRange(GameTestHelper helper) {
    helper.setBlock(TARGET, PEBlocks.DARK_MATTER_PEDESTAL.getBlock());
    DMPedestalBlockEntity pedestal =
        helper.getBlockEntity(TARGET, DMPedestalBlockEntity.class);
    ItemStack talisman = new ItemStack(PEItems.REPAIR_TALISMAN.get());
    pedestal.getInventory().setStackInSlot(0, talisman);
    pedestal.setActive(helper.getLevel(), pedestal.getBlockPos(), true);
    pedestal.setActivityCooldown(helper.getLevel(), pedestal.getBlockPos(), 37);

    BlockAccessor accessor = accessor(helper, pedestal);
    CompoundTag serverData = accessor.getServerData();
    JadeMachineDataProvider.INSTANCE.appendServerData(serverData, accessor);
    CompoundTag data = serverData.getCompoundOrEmpty(PEJadeConstants.MACHINE_DATA);
    helper.assertValueEqual(data.getStringOr("Kind", ""), "pedestal", "Jade machine type");
    helper.assertTrue(data.getBooleanOr("Active", false), "Jade receives the active state");
    helper.assertValueEqual(data.getIntOr("Cooldown", 0), 37, "Jade receives the cooldown");
    helper.assertValueEqual(data.getIntOr("RangeX", 0), 9, "Jade range width");
    helper.assertValueEqual(data.getIntOr("RangeY", 0), 9, "Jade range height");
    helper.assertValueEqual(data.getIntOr("RangeZ", 0), 9, "Jade range depth");
    ItemStack decoded =
        accessor
            .decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, data.get("Item"))
            .orElse(ItemStack.EMPTY);
    helper.assertTrue(
        ItemStack.isSameItemSameComponents(decoded, talisman),
        "Jade item stream preserves the pedestal item");
    List<Component> description =
        data.read("Description", ComponentSerialization.CODEC.listOf()).orElse(List.of());
    helper.assertTrue(!description.isEmpty(), "Jade receives translated pedestal effect details");
    helper.succeed();
  }

  @GameTest
  public void jadeRecognizesEveryProjectEMachineWithoutLeakingToVanillaBlocks(
      GameTestHelper helper) {
    Block[] blocks = {
        PEBlocks.ALCHEMICAL_CHEST.getBlock(),
        PEBlocks.CONDENSER.getBlock(),
        PEBlocks.CONDENSER_MK2.getBlock(),
        PEBlocks.DARK_MATTER_FURNACE.getBlock(),
        PEBlocks.RED_MATTER_FURNACE.getBlock(),
        PEBlocks.COLLECTOR.getBlock(),
        PEBlocks.COLLECTOR_MK2.getBlock(),
        PEBlocks.COLLECTOR_MK3.getBlock(),
        PEBlocks.RELAY.getBlock(),
        PEBlocks.RELAY_MK2.getBlock(),
        PEBlocks.RELAY_MK3.getBlock()
    };
    String[] kinds = {
        "chest", "condenser", "condenser", "furnace", "furnace", "collector",
        "collector", "collector", "relay", "relay", "relay"
    };
    for (int index = 0; index < blocks.length; index++) {
      helper.setBlock(TARGET, blocks[index]);
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TARGET));
      BlockAccessor accessor = accessor(helper, blockEntity);
      helper.assertTrue(
          JadeMachineDataProvider.INSTANCE.shouldRequestData(accessor),
          "Jade requests data for " + blocks[index]);
      JadeMachineDataProvider.INSTANCE.appendServerData(accessor.getServerData(), accessor);
      helper.assertValueEqual(
          accessor
              .getServerData()
              .getCompoundOrEmpty(PEJadeConstants.MACHINE_DATA)
              .getStringOr("Kind", ""),
          kinds[index],
          "Jade selects the correct machine presentation");
    }

    helper.setBlock(TARGET, Blocks.CHEST);
    BlockEntity vanillaChest = helper.getLevel().getBlockEntity(helper.absolutePos(TARGET));
    BlockAccessor accessor = accessor(helper, vanillaChest);
    helper.assertTrue(
        !JadeMachineDataProvider.INSTANCE.shouldRequestData(accessor),
        "ProjectE machine details do not attach to vanilla block entities");
    helper.succeed();
  }

  private static BlockAccessor accessor(GameTestHelper helper, BlockEntity blockEntity) {
    BlockPos pos = blockEntity.getBlockPos();
    return new BlockAccessorImpl.Builder()
        .level(helper.getLevel())
        .player(helper.makeMockPlayer(GameType.SURVIVAL))
        .serverData(new CompoundTag())
        .serverConnected(true)
        .showDetails(true)
        .hit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
        .blockState(blockEntity.getBlockState())
        .blockEntity((Supplier<BlockEntity>) () -> blockEntity)
        .build();
  }
}
