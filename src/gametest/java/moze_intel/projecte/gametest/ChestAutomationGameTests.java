package moze_intel.projecte.gametest;

import java.util.concurrent.atomic.AtomicBoolean;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.level.BlockEvent;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.block_entities.AlchBlockEntityChest;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity;
import moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.DMPedestalBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.RelayMK1BlockEntity;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.PlayerHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;

public class ChestAutomationGameTests {
  private static final BlockPos CHEST = new BlockPos(3, 3, 3);

  @GameTest
  public void nativeHoppersCanInsertAndExtractAlchemicalChestItems(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    helper.setBlock(CHEST.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
    helper.setBlock(CHEST.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    HopperBlockEntity input = helper.getBlockEntity(CHEST.above(), HopperBlockEntity.class);
    HopperBlockEntity output = helper.getBlockEntity(CHEST.below(), HopperBlockEntity.class);
    input.setItem(0, new ItemStack(Items.DIAMOND, 5));
    HopperBlockEntity.pushItemsTick(helper.getLevel(), input.getBlockPos(), input.getBlockState(), input);
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 1, "Native hopper inserts one item");
    helper.assertValueEqual(input.getItem(0).getCount(), 4, "Hopper source is debited once");
    helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), output), "Native hopper extracts from the chest");
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 0, "Extracted chest item is removed once");
    helper.assertValueEqual(output.countItem(Items.DIAMOND), 1, "Output hopper receives the actual item");
    helper.succeed();
  }

  @GameTest
  public void fullOutputHopperCannotConsumeChestItems(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    helper.setBlock(CHEST.below(), Blocks.HOPPER);
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    HopperBlockEntity output = helper.getBlockEntity(CHEST.below(), HopperBlockEntity.class);
    for (int slot = 0; slot < output.getContainerSize(); slot++) {
      output.setItem(slot, new ItemStack(Items.STONE, 64));
    }
    var inventory = (IItemHandlerModifiable) chest.getInventory();
    inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
    inventory.setStackInSlot(103, new ItemStack(Items.GOLD_INGOT));
    helper.assertTrue(!HopperBlockEntity.suckInItems(helper.getLevel(), output),
        "Full hopper rejects extraction");
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 3,
        "Failed extraction restores a multi-item stack");
    helper.assertValueEqual(count(chest.getInventory(), Items.GOLD_INGOT), 1,
        "Failed extraction restores a single-item stack");
    helper.assertValueEqual(output.countItem(Items.STONE), 320, "Full output is unchanged");
    output.setItem(0, ItemStack.EMPTY);
    helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), output),
        "Transfer resumes when space is available");
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 2,
        "Successful extraction debits only one item");
    helper.assertValueEqual(output.countItem(Items.DIAMOND), 1,
        "Successful extraction credits only one item");
    helper.succeed();
  }

  @GameTest
  public void droppersCanInsertChestItemsWithComponents(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    BlockPos dropperPos = CHEST.south();
    helper.setBlock(dropperPos,
        Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH));
    DropperBlockEntity dropper = helper.getBlockEntity(dropperPos, DropperBlockEntity.class);
    ItemStack named = new ItemStack(Items.DIAMOND, 2);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Automated chest transfer"));
    dropper.setItem(0, named);
    dropper.getBlockState().tick(helper.getLevel(), dropper.getBlockPos(), helper.getLevel().getRandom());
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 1,
        "Dropper inserts exactly one item into the chest");
    helper.assertValueEqual(dropper.getItem(0).getCount(), 1, "Dropper is debited once");
    helper.assertValueEqual(chest.getInventory().getStackInSlot(0).getComponents(), named.getComponents(),
        "Automation preserves item components");
    helper.assertValueEqual(dropped(helper, Items.DIAMOND), 0,
        "Dropper uses the chest rather than ejecting an item entity");
    helper.succeed();
  }

  @GameTest
  public void hopperMinecartCanExtractAlchemicalChestItems(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    ((IItemHandlerModifiable) chest.getInventory()).setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 3));
    MinecartHopper cart = new MinecartHopper(EntityTypes.HOPPER_MINECART, helper.getLevel());
    // A minecart's hopper Y is already offset by +0.5; use the track-level position.
    cart.setPos(Vec3.atBottomCenterOf(helper.absolutePos(CHEST.below())));
    helper.getLevel().addFreshEntity(cart);
    helper.assertTrue(cart.suckInItems(), "Hopper minecart can access the native chest container");
    helper.assertValueEqual(cart.countItem(Items.GOLD_INGOT), 1, "Minecart receives exactly one item");
    helper.assertValueEqual(count(chest.getInventory(), Items.GOLD_INGOT), 2, "Minecart extraction is conserved");
    helper.succeed();
  }

  @GameTest
  public void fabricStorageTransactionsConserveChestItemsAcrossSides(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    ItemVariant diamond = ItemVariant.of(Items.DIAMOND);
    Storage<ItemVariant> north = storage(helper, Direction.NORTH);
    Storage<ItemVariant> south = storage(helper, Direction.SOUTH);
    try (Transaction outer = Transaction.openOuter()) {
      helper.assertValueEqual(north.insert(diamond, 9, outer), 9L, "Fabric insertion is accepted");
      try (Transaction inner = outer.openNested()) {
        helper.assertValueEqual(south.extract(diamond, 4, inner), 4L, "Another face shares the same inventory");
      }
      helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 9, "Nested abort restores extraction");
    }
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 0, "Outer abort restores all insertion");
    try (Transaction transaction = Transaction.openOuter()) {
      north.insert(diamond, 9, transaction);
      transaction.commit();
    }
    try (Transaction transaction = Transaction.openOuter()) {
      helper.assertValueEqual(south.extract(diamond, 4, transaction), 4L, "Committed extraction count");
      transaction.commit();
    }
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 5, "Committed balance is correct");
    helper.succeed();
  }

  @GameTest
  public void condenserAutomationPreservesInputOutputAndGhostRules(GameTestHelper helper) {
    for (boolean mk2 : new boolean[] {false, true}) {
      CondenserBlockEntity condenser = condenser(helper, mk2);
      Storage<ItemVariant> storage = storage(helper, Direction.UP);
      try (Transaction transaction = Transaction.openOuter()) {
        helper.assertValueEqual(storage.insert(ItemVariant.of(Items.DIAMOND), 1, transaction), 0L, "Automation rejects the target as input");
        helper.assertValueEqual(storage.insert(ItemVariant.of(Items.BEDROCK), 1, transaction), 0L, "Automation rejects valueless input");
        helper.assertValueEqual(storage.insert(ItemVariant.of(Items.COAL), 3, transaction), 3L, "EMC input is accepted");
        helper.assertValueEqual(storage.extract(ItemVariant.of(Items.COAL), 1, transaction), 0L, "Automation cannot extract unsold input");
        transaction.commit();
      }
      condenser.getOutput().setStackInSlot(mk2 ? 0 : 1, new ItemStack(Items.DIAMOND, 2));
      try (Transaction transaction = Transaction.openOuter()) {
        helper.assertValueEqual(storage.extract(ItemVariant.of(Items.DIAMOND), 1, transaction), 1L, "Generated output can be extracted");
        transaction.commit();
      }
      helper.assertValueEqual(count(condenser.getOutput(), Items.DIAMOND), 1, "Output is removed once");
      helper.assertValueEqual(count(condenser.getInput(), Items.COAL), 3, "Input remains untouched");
      helper.assertTrue(condenser.getLockInfo() != null, "Ghost target cannot be extracted");
    }
    helper.succeed();
  }

  @GameTest
  public void nativeCondenserHoppersKeepInputsAndGhostTargetProtected(GameTestHelper helper) {
    for (boolean mk2 : new boolean[] {false, true}) {
      CondenserBlockEntity condenser = condenser(helper, mk2);
      helper.setBlock(CHEST.above(),
          Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
      helper.setBlock(CHEST.below(),
          Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
      HopperBlockEntity input = helper.getBlockEntity(CHEST.above(), HopperBlockEntity.class);
      HopperBlockEntity output = helper.getBlockEntity(CHEST.below(), HopperBlockEntity.class);
      input.setItem(0, new ItemStack(Items.COAL, 5));
      HopperBlockEntity.pushItemsTick(helper.getLevel(), input.getBlockPos(), input.getBlockState(), input);
      helper.assertValueEqual(count(condenser.getInput(), Items.COAL), 1,
          "Native hopper inserts into the actual input inventory");
      helper.assertValueEqual(input.countItem(Items.COAL), 4, "Input hopper is debited once");
      helper.assertTrue(!HopperBlockEntity.suckInItems(helper.getLevel(), output),
          "Native hopper cannot extract condenser inputs");
      condenser.getOutput().setStackInSlot(mk2 ? 0 : 1, new ItemStack(Items.DIAMOND, 2));
      helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), output),
          "Native hopper can extract generated output");
      helper.assertValueEqual(output.countItem(Items.DIAMOND), 1, "Output hopper receives one item");
      helper.assertValueEqual(count(condenser.getOutput(), Items.DIAMOND), 1, "Output is debited once");
      helper.assertValueEqual(count(condenser.getInput(), Items.COAL), 1, "Input remains protected");
      helper.assertTrue(condenser.getLockInfo() != null, "Ghost target survives extraction");
      // Remove the fixture without leaving hopper contents for the next tier to pick up.
      input.clearContent();
      output.clearContent();
      helper.setBlock(CHEST.above(), Blocks.AIR);
      helper.setBlock(CHEST.below(), Blocks.AIR);
    }
    helper.succeed();
  }

  @GameTest
  public void destroyingAlchemicalChestDropsAllContentsOnceWithComponents(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    var inventory = (IItemHandlerModifiable) chest.getInventory();
    ItemStack named = new ItemStack(Items.DIAMOND, 7);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved chest contents"));
    inventory.setStackInSlot(0, named);
    inventory.setStackInSlot(103, new ItemStack(Items.COAL, 11));
    helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(CHEST), true), "Chest break succeeds");
    helper.assertValueEqual(dropped(helper, Items.DIAMOND), 7, "Chest diamonds drop once");
    helper.assertValueEqual(dropped(helper, Items.COAL), 11, "The last slot is also dropped");
    helper.assertValueEqual(dropped(helper, PEBlocks.ALCHEMICAL_CHEST.getBlock().asItem()), 1,
        "The chest block itself still drops");
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 0, "Dropped items are cleared from old inventory");
    helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
        .filter(entity -> entity.getItem().is(Items.DIAMOND))
        .allMatch(entity -> entity.getItem().getComponents().equals(named.getComponents())), "Dropped components are unchanged");
    helper.getLevel().destroyBlock(helper.absolutePos(CHEST), true);
    helper.assertValueEqual(dropped(helper, Items.DIAMOND), 7, "Repeated destruction cannot duplicate contents");
    helper.succeed();
  }

  @GameTest
  public void nativePlayerBreakDropsChestContentsInSurvivalAndCreative(GameTestHelper helper) {
    for (GameType mode : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
      clearDrops(helper);
      helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
      AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
      ((IItemHandlerModifiable) chest.getInventory()).setStackInSlot(103, new ItemStack(Items.DIAMOND, 7));
      ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(mode);
      player.setPos(Vec3.atCenterOf(helper.absolutePos(CHEST)).add(0, 0, 2));
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
      helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(CHEST)),
          "Native player break succeeds in " + mode);
      helper.assertValueEqual(dropped(helper, Items.DIAMOND), 7,
          "Native player break preserves the entire chest inventory in " + mode);
      helper.assertValueEqual(dropped(helper, PEBlocks.ALCHEMICAL_CHEST.getBlock().asItem()),
          mode == GameType.CREATIVE ? 0 : 1, "Native block-drop rules are retained");
      helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 0,
          "The removed chest does not retain a second copy");
    }
    helper.succeed();
  }

  @GameTest
  public void chestReplacementRestoresOrDropsContentsAfterPlacementDecision(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    ItemStack named = new ItemStack(Items.DIAMOND, 7);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Canceled replacement contents"));
    ((IItemHandlerModifiable) chest.getInventory()).setStackInSlot(0, named);
    ((IItemHandlerModifiable) chest.getInventory()).setStackInSlot(103, new ItemStack(Items.COAL, 11));
    chest.insertEmc(123, EmcAction.EXECUTE);
    ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    player.setPos(Vec3.atCenterOf(helper.absolutePos(CHEST)).add(0, 0, 2));
    AtomicBoolean deny = new AtomicBoolean(true);
    AtomicBoolean invoked = new AtomicBoolean();
    NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, event -> {
      if (event.getEntity() == player) {
        invoked.set(true);
        helper.assertValueEqual(dropped(helper, Items.DIAMOND), 0,
            "Contents are deferred until the placement decision");
        event.setCanceled(deny.get());
      }
    });
    try {
      helper.assertTrue(!PlayerHelper.checkedReplaceBlock(player, helper.getLevel(),
          helper.absolutePos(CHEST), Blocks.COBBLESTONE.defaultBlockState()),
          "Canceled replacement reports failure");
      helper.assertTrue(invoked.get(), "Actual placement event was dispatched");
      helper.assertBlockPresent(PEBlocks.ALCHEMICAL_CHEST.getBlock(), CHEST);
      AlchBlockEntityChest restored = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
      helper.assertValueEqual(count(restored.getInventory(), Items.DIAMOND), 7,
          "Canceled replacement restores the inventory");
      helper.assertValueEqual(count(restored.getInventory(), Items.COAL), 11,
          "Canceled replacement restores the last slot");
      helper.assertValueEqual(restored.getInventory().getStackInSlot(0).getComponents(), named.getComponents(),
          "Restored stack retains its components");
      helper.assertValueEqual(restored.getStoredEmc(), 123L, "Canceled replacement restores EMC");
      helper.assertValueEqual(dropped(helper, Items.DIAMOND), 0,
          "Canceled replacement cannot duplicate restored items");
      deny.set(false);
      helper.assertTrue(PlayerHelper.checkedReplaceBlock(player, helper.getLevel(),
          helper.absolutePos(CHEST), Blocks.COBBLESTONE.defaultBlockState()),
          "Allowed replacement succeeds");
      helper.assertBlockPresent(Blocks.COBBLESTONE, CHEST);
      helper.assertValueEqual(dropped(helper, Items.DIAMOND), 7,
          "Allowed replacement releases contents exactly once");
      helper.assertValueEqual(dropped(helper, Items.COAL), 11,
          "Allowed replacement releases the last slot exactly once");
    } finally {
      deny.set(false);
    }
    helper.succeed();
  }

  @GameTest
  public void destroyingMachinesDropsRealInventoriesWithoutGhostItems(GameTestHelper helper) {
    Block[] machines = {
        PEBlocks.COLLECTOR.getBlock(), PEBlocks.COLLECTOR_MK2.getBlock(), PEBlocks.COLLECTOR_MK3.getBlock(),
        PEBlocks.RELAY.getBlock(), PEBlocks.RELAY_MK2.getBlock(), PEBlocks.RELAY_MK3.getBlock(),
        PEBlocks.DARK_MATTER_FURNACE.getBlock(), PEBlocks.RED_MATTER_FURNACE.getBlock(),
        PEBlocks.DARK_MATTER_PEDESTAL.getBlock()
    };
    for (Block machine : machines) {
      clearDrops(helper);
      helper.setBlock(CHEST, machine);
      var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(CHEST));
      int coal;
      int gold;
      if (blockEntity instanceof CollectorMK1BlockEntity collector) {
        putLast(collector.getInput(), new ItemStack(Items.COAL, 3));
        var aux = (IItemHandlerModifiable) collector.getAux();
        aux.setStackInSlot(0, new ItemStack(Items.COAL, 5));
        aux.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, 7));
        aux.setStackInSlot(CollectorMK1BlockEntity.LOCK_SLOT, new ItemStack(Items.DIAMOND));
        coal = 8;
        gold = 7;
      } else if (blockEntity instanceof RelayMK1BlockEntity relay) {
        putLast(relay.getInput(), new ItemStack(Items.COAL, 3));
        putLast(relay.getOutput(), new ItemStack(Items.GOLD_INGOT, 7));
        coal = 3;
        gold = 7;
      } else if (blockEntity instanceof DMFurnaceBlockEntity furnace) {
        putLast(furnace.getInput(), new ItemStack(Items.COAL, 3));
        putLast(furnace.getFuel(), new ItemStack(Items.COAL, 5));
        putLast(furnace.getOutput(), new ItemStack(Items.GOLD_INGOT, 7));
        coal = 8;
        gold = 7;
      } else {
        DMPedestalBlockEntity pedestal = (DMPedestalBlockEntity) blockEntity;
        putLast(pedestal.getInventory(), new ItemStack(Items.GOLD_INGOT));
        coal = 0;
        gold = 1;
      }
      helper.getLevel().destroyBlock(helper.absolutePos(CHEST), true);
      helper.assertValueEqual(dropped(helper, Items.COAL), coal,
          "Stored inputs and fuel drop once for " + machine);
      helper.assertValueEqual(dropped(helper, Items.GOLD_INGOT), gold,
          "Stored outputs drop once for " + machine);
      helper.assertValueEqual(dropped(helper, Items.DIAMOND), 0,
          "Ghost lock is not a stored item for " + machine);
    }
    helper.succeed();
  }

  @GameTest
  public void destroyingCondensersDropsBothInventoriesButNotGhostTarget(GameTestHelper helper) {
    for (boolean mk2 : new boolean[] {false, true}) {
      clearDrops(helper);
      CondenserBlockEntity condenser = condenser(helper, mk2);
      condenser.getInput().setStackInSlot(0, new ItemStack(Items.COAL, 3));
      condenser.getOutput().setStackInSlot(mk2 ? 0 : 1, new ItemStack(Items.DIAMOND, 5));
      helper.getLevel().destroyBlock(helper.absolutePos(CHEST), true);
      helper.assertValueEqual(dropped(helper, Items.COAL), 3, "Condenser inputs are retained");
      helper.assertValueEqual(dropped(helper, Items.DIAMOND), 5, "Paid output drops without an extra ghost item");
      helper.assertValueEqual(count(condenser.getInput(), Items.COAL), 0, "Dropped input is cleared");
      helper.assertValueEqual(count(condenser.getOutput(), Items.DIAMOND), 0, "Dropped output is cleared");
    }
    helper.succeed();
  }

  @GameTest
  public void changingChestStateKeepsInventoryWithoutDroppingItems(GameTestHelper helper) {
    helper.setBlock(CHEST, PEBlocks.ALCHEMICAL_CHEST.getBlock());
    AlchBlockEntityChest chest = helper.getBlockEntity(CHEST, AlchBlockEntityChest.class);
    ((IItemHandlerModifiable) chest.getInventory()).setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
    helper.getLevel().setBlockAndUpdate(helper.absolutePos(CHEST), chest.getBlockState()
        .setValue(moze_intel.projecte.gameObjs.blocks.BlockDirection.FACING, Direction.EAST));
    helper.assertValueEqual(dropped(helper, Items.DIAMOND), 0, "Rotation is not destruction");
    helper.assertValueEqual(count(chest.getInventory(), Items.DIAMOND), 3, "Rotation retains inventory");
    helper.succeed();
  }

  private static CondenserBlockEntity condenser(GameTestHelper helper, boolean mk2) {
    helper.setBlock(CHEST, mk2 ? PEBlocks.CONDENSER_MK2.getBlock() : PEBlocks.CONDENSER.getBlock());
    CondenserBlockEntity condenser = helper.getBlockEntity(CHEST, CondenserBlockEntity.class);
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.setPos(Vec3.atCenterOf(helper.absolutePos(CHEST)).add(0, 0, 2));
    CondenserContainer menu = (CondenserContainer) condenser.createMenu(23, player.getInventory(), player);
    player.containerMenu = menu;
    menu.setCarried(new ItemStack(Items.DIAMOND));
    menu.clicked(0, 0, ContainerInput.PICKUP, player);
    helper.assertTrue(condenser.getLockInfo() != null, "Condenser target is set");
    return condenser;
  }

  private static Storage<ItemVariant> storage(GameTestHelper helper, Direction side) {
    var storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(CHEST), side);
    helper.assertTrue(storage != null, "Chest exposes Fabric ItemStorage on face " + side);
    return storage;
  }

  private static int count(IItemHandler inventory, Item item) {
    int result = 0;
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      ItemStack stack = inventory.getStackInSlot(slot);
      if (stack.is(item)) {
        result += stack.getCount();
      }
    }
    return result;
  }

  private static void putLast(IItemHandler inventory, ItemStack stack) {
    ((IItemHandlerModifiable) inventory).setStackInSlot(inventory.getSlots() - 1, stack);
  }

  private static int dropped(GameTestHelper helper, Item item) {
    return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
        .map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
  }

  private static void clearDrops(GameTestHelper helper) {
    helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).forEach(ItemEntity::discard);
  }
}
