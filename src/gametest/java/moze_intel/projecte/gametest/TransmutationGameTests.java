package moze_intel.projecte.gametest;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.items.ComponentItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.compat.items.wrapper.PlayerMainInvWrapper;
import moze_intel.projecte.compat.items.wrapper.RangedWrapper;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.block_entities.WrappedItemHandler;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class TransmutationGameTests {
  private static final int OUTPUT_SLOT = 11;

  @GameTest
  public void shiftRightClickAddsOnlyAffordableItemsToExistingStack(GameTestHelper helper) {
    ItemStack target = new ItemStack(Items.DIAMOND);
    BigInteger price = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(target));
    for (int affordable : new int[] {0, 1, 2, 7, 63, 64}) {
      BigInteger funds = price.multiply(BigInteger.valueOf(affordable)).add(BigInteger.valueOf(17));
      Fixture f = fixture(helper, target, funds);
      f.player.getInventory().setItem(0, target.copy());
      BigInteger before = total(f, target);
      f.menu.clicked(OUTPUT_SLOT, 1, ContainerInput.QUICK_MOVE, f.player);
      helper.assertValueEqual(count(f.player, target), 1 + affordable,
          "Shift-right-click inserts only funded output");
      helper.assertValueEqual(f.menu.transmutationInventory.getAvailableEmc(), BigInteger.valueOf(17),
          "Only the actual purchase cost is deducted");
      helper.assertValueEqual(total(f, target), before, "EMC plus purchased item value is conserved");
      for (int attempt = 0; attempt < 8; attempt++) {
        f.menu.clicked(OUTPUT_SLOT, 1, ContainerInput.QUICK_MOVE, f.player);
      }
      helper.assertValueEqual(total(f, target), before, "Repeated unfunded clicks cannot create items");
      helper.assertValueEqual(count(f.player, target), 1 + affordable, "Unfunded clicks keep the stack unchanged");
    }
    helper.succeed();
  }

  @GameTest
  @SuppressWarnings("removal")
  public void actualRepeatedShiftRightClickPacketsCannotDuplicateOutput(GameTestHelper helper) {
    ServerPlayer player = helper.makeMockServerPlayerInLevel();
    ItemStack target = new ItemStack(Items.DIAMOND);
    try {
      Fixture f = fixture(helper, player, target,
          BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(target)).add(BigInteger.valueOf(17)));
      player.getInventory().setItem(0, target.copy());
      BigInteger before = total(f, target);
      ServerboundContainerClickPacket click = new ServerboundContainerClickPacket(
          f.menu.containerId, f.menu.getStateId(), (short) OUTPUT_SLOT, (byte) 1,
          ContainerInput.QUICK_MOVE, new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY);
      for (int attempt = 0; attempt < 20; attempt++) {
        player.connection.handleContainerClick(click);
      }
      helper.assertValueEqual(count(player, target), 2, "Actual shift-right-click packets buy one item");
      helper.assertValueEqual(f.menu.transmutationInventory.getAvailableEmc(), BigInteger.valueOf(17),
          "Replayed packets cannot bypass the available EMC");
      helper.assertValueEqual(total(f, target), before, "Server-authoritative packet handling conserves EMC");
    } finally {
      player.discard();
      helper.getLevel().getServer().getPlayerList().remove(player);
    }
    helper.succeed();
  }

  @GameTest
  public void fullInventoryChargesOnlyForAcceptedOutput(GameTestHelper helper) {
    ItemStack target = new ItemStack(Items.DIAMOND);
    BigInteger price = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(target));
    Fixture f = fixture(helper, target, price.multiply(BigInteger.TWO).add(BigInteger.valueOf(17)));
    for (int slot = 0; slot < f.player.getInventory().getNonEquipmentItems().size(); slot++) {
      f.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
    }
    f.player.getInventory().setItem(0, target.copyWithCount(63));
    BigInteger before = total(f, target);
    f.menu.clicked(OUTPUT_SLOT, 1, ContainerInput.QUICK_MOVE, f.player);
    helper.assertValueEqual(count(f.player, target), 64, "Only one item fits in the remaining capacity");
    helper.assertValueEqual(f.menu.transmutationInventory.getAvailableEmc(), price.add(BigInteger.valueOf(17)),
        "Unaccepted output is not charged");
    f.menu.clicked(OUTPUT_SLOT, 1, ContainerInput.QUICK_MOVE, f.player);
    helper.assertValueEqual(total(f, target), before, "A full inventory cannot lose EMC or create output");
    helper.assertTrue(f.menu.getCarried().isEmpty(), "Shift transfer does not move output to the cursor");
    helper.succeed();
  }

  @GameTest
  public void kleinStarFundsAndExistingOutputRemainConserved(GameTestHelper helper) {
    ItemStack target = new ItemStack(Items.DIAMOND);
    long price = IEMCProxy.INSTANCE.getValue(target);
    Fixture f = fixture(helper, target, BigInteger.valueOf(price / 2 + 17));
    ItemStack star = new ItemStack(PEItems.KLEIN_STAR_EIN.get());
    PEItems.KLEIN_STAR_EIN.get().insertEmc(star, price - price / 2, EmcAction.EXECUTE);
    ((IItemHandlerModifiable) f.menu.transmutationInventory.provider.getInputAndLocks()).setStackInSlot(0, star);
    f.menu.transmutationInventory.writeIntoOutputSlot(0, target.copy());
    f.player.getInventory().setItem(0, target.copy());
    BigInteger before = total(f, target);
    f.menu.clicked(OUTPUT_SLOT, 1, ContainerInput.QUICK_MOVE, f.player);
    helper.assertValueEqual(count(f.player, target), 2, "Combined table and Klein Star funds buy one item");
    helper.assertValueEqual(f.menu.transmutationInventory.getAvailableEmc(), BigInteger.valueOf(17),
        "Klein Star and table balance pay exactly the purchase cost");
    helper.assertValueEqual(total(f, target), before, "Stored-item EMC is also conserved");
    helper.succeed();
  }

  @GameTest
  public void valuelessStaleOutputCannotBeTakenThroughOtherClickModes(GameTestHelper helper) {
    var original = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    ItemStack target = new ItemStack(Items.DIAMOND);
    long price = IEMCProxy.INSTANCE.getValue(target);
    Fixture f = fixture(helper, target, BigInteger.valueOf(price));
    var changed = new Object2LongOpenHashMap<ItemInfo>(original);
    changed.put(ItemInfo.fromStack(target), 0L);
    EMCMappingHandler.updateEmcValues(changed);
    try {
      for (ContainerInput click : new ContainerInput[] {ContainerInput.PICKUP, ContainerInput.QUICK_MOVE,
          ContainerInput.SWAP, ContainerInput.THROW}) {
        f.menu.clicked(OUTPUT_SLOT, click == ContainerInput.SWAP ? 0 : 1, click, f.player);
      }
      helper.assertTrue(f.menu.getCarried().isEmpty(), "A zero-value stale output cannot be taken onto the cursor");
      helper.assertValueEqual(count(f.player, target), 0, "Other click modes cannot take a stale free item");
      int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
          .map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
      helper.assertValueEqual(dropped, 0, "Throwing a stale output cannot create free item entities");
      helper.assertValueEqual(f.menu.transmutationInventory.getAvailableEmc(), BigInteger.valueOf(price),
          "Rejected stale output keeps the balance unchanged");
    } finally {
      EMCMappingHandler.updateEmcValues(original);
    }
    helper.succeed();
  }

  @GameTest
  public void densityGemInPlayerInventoryAddsOnlyPaidOutput(GameTestHelper helper) {
    ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    ItemStack gem = new ItemStack(PEItems.GEM_OF_ETERNAL_DENSITY.get());
    long price = IEMCProxy.INSTANCE.getValue(Items.IRON_INGOT);
    gem.set(PEDataComponentTypes.ACTIVE, true);
    gem.set(PEDataComponentTypes.STORED_EMC, price + 17);
    player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT));
    player.getInventory().setItem(9, gem);
    for (int tick = 0; tick < 5; tick++) {
      gem.getItem().inventoryTick(gem, helper.getLevel(), player, EquipmentSlot.MAINHAND);
    }
    helper.assertValueEqual(count(player, new ItemStack(Items.IRON_INGOT)), 2,
        "The same player-inventory merge path cannot amplify gem output");
    helper.assertValueEqual(gem.get(PEDataComponentTypes.STORED_EMC), 17L,
        "Density gem retains the unpaid EMC remainder");
    helper.succeed();
  }

  @GameTest
  public void inventoryMergeImplementationsMatchSimulationAndKeepComponents(GameTestHelper helper) {
    ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    for (int maximum : new int[] {16, 64}) {
      for (int existingCount : new int[] {1, maximum / 2, maximum - 1, maximum}) {
        for (int incomingCount : new int[] {1, 2, 7, 64}) {
          ItemStack source = new ItemStack(Items.DIAMOND, incomingCount);
          source.set(DataComponents.MAX_STACK_SIZE, maximum);
          source.set(DataComponents.CUSTOM_NAME, Component.literal("Conserved merge"));
          List<IItemHandlerModifiable> inventories = List.of(
              new PlayerMainInvWrapper(player.getInventory()),
              new ItemStackHandler(2),
              new ComponentItemHandler(new ItemStack(Items.SHULKER_BOX), DataComponents.CONTAINER, 2),
              new CombinedInvWrapper(new ItemStackHandler(1), new ItemStackHandler(1)),
              new RangedWrapper(new ItemStackHandler(3), 1, 3),
              new WrappedItemHandler(new ItemStackHandler(2), WrappedItemHandler.WriteMode.IN));
          for (IItemHandlerModifiable inventory : inventories) {
            inventory.setStackInSlot(0, source.copyWithCount(existingCount));
            ItemStack simulated = inventory.insertItem(0, source, true);
            helper.assertValueEqual(inventory.getStackInSlot(0).getCount(), existingCount,
                "Simulation cannot add items in " + inventory.getClass().getSimpleName());
            ItemStack remainder = inventory.insertItem(0, source, false);
            helper.assertValueEqual(remainder.getCount(), simulated.getCount(),
                "Actual insertion matches simulation in " + inventory.getClass().getSimpleName());
            helper.assertValueEqual(inventory.getStackInSlot(0).getCount() + remainder.getCount(),
                existingCount + incomingCount, "Every insertion conserves item count");
            helper.assertValueEqual(inventory.getStackInSlot(0).getComponents(), source.getComponents(),
                "Merging retains components");
            helper.assertValueEqual(source.getCount(), incomingCount, "Source stack is not modified");
            int stored = inventory.getStackInSlot(0).getCount();
            helper.assertTrue(inventory.extractItem(0, -1, false).isEmpty(),
                "Invalid extraction cannot produce items");
            helper.assertValueEqual(inventory.getStackInSlot(0).getCount(), stored,
                "Invalid extraction cannot grow the source stack");
          }
        }
      }
    }
    helper.succeed();
  }

  private static Fixture fixture(GameTestHelper helper, ItemStack target, BigInteger funds) {
    return fixture(helper, (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL), target, funds);
  }

  private static Fixture fixture(GameTestHelper helper, ServerPlayer player, ItemStack target, BigInteger funds) {
    if (player.connection == null) {
      player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
          new Connection(PacketFlow.SERVERBOUND), player,
          CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
        @Override
        public void send(Packet<?> packet) {}
      };
    }
    var knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    helper.assertTrue(knowledge != null, "Player knowledge is available");
    knowledge.addKnowledge(ItemInfo.fromStack(target));
    knowledge.setEmc(funds.max(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(target))));
    TransmutationContainer menu = new TransmutationContainer(41, player.getInventory());
    player.containerMenu = menu;
    menu.transmutationInventory.writeIntoOutputSlot(0, target.copyWithCount(1));
    knowledge.setEmc(funds);
    helper.assertTrue(menu.getSlot(OUTPUT_SLOT).hasItem(), "Server output corresponds to a learned target");
    return new Fixture(player, menu);
  }

  private static int count(ServerPlayer player, ItemStack target) {
    return player.getInventory().getNonEquipmentItems().stream()
        .filter(stack -> ItemStack.isSameItemSameComponents(stack, target)).mapToInt(ItemStack::getCount).sum();
  }

  private static BigInteger total(Fixture fixture, ItemStack target) {
    return fixture.menu.transmutationInventory.getAvailableEmc()
        .add(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(target)).multiply(BigInteger.valueOf(count(fixture.player, target))));
  }

  private record Fixture(ServerPlayer player, TransmutationContainer menu) {}
}
