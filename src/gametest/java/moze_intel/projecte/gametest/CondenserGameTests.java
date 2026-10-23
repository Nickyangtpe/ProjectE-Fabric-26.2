package moze_intel.projecte.gametest;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.event.PlayerAttemptCondenserSetEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.CondenserMK2BlockEntity;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.container.CondenserMK2Container;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.container.UpdateCondenserLockPKT;
import moze_intel.projecte.network.packets.to_client.container.UpdateWindowLongPKT;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

public class CondenserGameTests implements CustomTestMethodInvoker {
  private static final BlockPos POSITION = BlockPos.ZERO;

  @Override
  public void invokeTestMethod(GameTestHelper helper, Method method)
      throws ReflectiveOperationException {
    helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DIAMOND), 8192L, "Test diamond EMC");
    helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.GOLD_INGOT), 2048L, "Test gold EMC");
    helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.BEDROCK), 0L, "Valueless test target");
    method.invoke(this, helper);
  }

  @GameTest
  public void onePaymentCreatesOneItemEvenWhenOutputAlreadyExists(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND, 64));
      f.condenser.getOutput().setStackInSlot(0, new ItemStack(Items.DIAMOND));
      long budget = value(Items.DIAMOND) + 37;
      f.condenser.insertEmc(budget, EmcAction.EXECUTE);
      BigInteger before = total(f.condenser);

      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 2, "One new diamond");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 37L, "Exactly one diamond's cost");
      helper.assertValueEqual(total(f.condenser), before, "Output generation conserves EMC");
      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 2, "No free next output");
    }
    helper.succeed();
  }

  @GameTest
  public void highToLowAndLowToHighPreserveRemainderAndCost(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      long budget = 2 * value(Items.DIAMOND) + 731;
      f.condenser.insertEmc(budget, EmcAction.EXECUTE);
      select(helper, f, new ItemStack(Items.GOLD_INGOT, 64));
      helper.assertValueEqual(f.condenser.getStoredEmc(), budget, "Switching does not spend EMC");
      helper.assertValueEqual(f.condenser.requiredEmc, value(Items.GOLD_INGOT), "New target cost");
      int gold = (int) (budget / value(Items.GOLD_INGOT));
      for (int i = 0; i < gold; i++) {
        tick(f.condenser);
      }
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), gold, "Gold count");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 731L, "Remainder after low target");

      clear(f);
      clearInventory(f.condenser.getInput());
      if (mk2) {
        clearInventory(f.condenser.getOutput());
      }
      select(helper, f, new ItemStack(Items.DIAMOND, 3));
      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 0, "New higher price applies");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 731L, "Insufficient balance is preserved");
      f.condenser.insertEmc(value(Items.DIAMOND) - 731, EmcAction.EXECUTE);
      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 1, "One fully funded diamond");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 0L, "Higher price is charged once");
    }
    helper.succeed();
  }

  @GameTest
  public void repeatedSwitchesCannotAmplifyOldOutputIntoMoreEmc(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(2 * value(Items.DIAMOND) + 91, EmcAction.EXECUTE);
      BigInteger supplied = total(f.condenser);
      for (int cycle = 0; cycle < 100; cycle++) {
        select(helper, f, new ItemStack(cycle % 2 == 0 ? Items.GOLD_INGOT : Items.DIAMOND, 64));
        if (mk2) {
          // MK2 keeps separate outputs. Re-feed them through its actual automation input.
          for (int slot = 0; slot < f.condenser.getOutput().getSlots(); slot++) {
            ItemStack stack = f.condenser.getOutput().getStackInSlot(slot);
            if (!stack.isEmpty() && !f.condenser.isStackEqualToLock(stack)) {
              ItemStack moved = f.condenser.getOutput().extractItem(slot, stack.getCount(), false);
              ItemStack remainder = CondenserBlockEntity.INVENTORY_PROVIDER
                  .getCapability(f.condenser, null).insertItem(slot, moved, false);
              helper.assertTrue(remainder.isEmpty(), "Old output can be used as EMC input");
            }
          }
        }
        for (int i = 0; i < 10; i++) {
          tick(f.condenser);
          helper.assertValueEqual(total(f.condenser), supplied, "Conservation at switch " + cycle);
        }
      }
    }
    helper.succeed();
  }

  @GameTest
  public void rapidSwitchesAndAllGhostClickModesKeepItemsAndBalance(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(777, EmcAction.EXECUTE);
      for (int i = 0; i < 200; i++) {
        Item item = i % 2 == 0 ? Items.DIAMOND : Items.GOLD_INGOT;
        select(helper, f, new ItemStack(item, i % 64 + 1));
        helper.assertValueEqual(f.condenser.requiredEmc, value(item), "Immediate new price");
        helper.assertValueEqual(f.condenser.getStoredEmc(), 777L, "No charge for a ghost click");
      }
      for (ContainerInput input : ContainerInput.values()) {
        f.menu.setCarried(new ItemStack(Items.DIAMOND, 23));
        f.menu.clicked(0, 0, input, f.player);
        f.menu.clicked(0, 0, input, f.player);
        helper.assertValueEqual(f.menu.getCarried().getCount(), 23, "Ghost slot never consumes cursor items");
        helper.assertValueEqual(f.condenser.getStoredEmc(), 777L, "Click modes never charge EMC");
        helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 0, "Ghost is not output");
        helper.assertTrue(f.menu.quickMoveStack(f.player, 0).isEmpty(), "Ghost cannot be shift-extracted");
      }
    }
    helper.succeed();
  }

  @GameTest
  public void valuelessAndClearedTargetsPauseWithoutLosingStoredEmc(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(515, EmcAction.EXECUTE);
      f.condenser.getInput().setStackInSlot(0, new ItemStack(Items.COAL, 8));
      clear(f);
      for (int i = 0; i < 5; i++) {
        tick(f.condenser);
      }
      helper.assertValueEqual(f.condenser.getStoredEmc(), 515L, "Cleared target retains balance");
      helper.assertValueEqual(count(f.condenser.getInput(), Items.COAL), 8, "No input consumed when cleared");
      f.menu.setCarried(new ItemStack(Items.BEDROCK, 64));
      f.menu.clicked(0, 0, ContainerInput.PICKUP, f.player);
      helper.assertTrue(f.condenser.getLockInfo() == null, "Valueless target is hidden, like Forge");
      helper.assertValueEqual(f.condenser.requiredEmc, 0L, "Valueless target cannot be purchased");
      helper.assertValueEqual(f.condenser.insertEmc(10, EmcAction.EXECUTE), 0L, "Paused EMC acceptance");
      tick(f.condenser);
      helper.assertValueEqual(f.condenser.getStoredEmc(), 515L, "Valueless target preserves balance");
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.BEDROCK), 0, "No zero-cost output");
      select(helper, f, new ItemStack(Items.GOLD_INGOT));
      BigInteger before = total(f.condenser);
      tick(f.condenser);
      helper.assertValueEqual(total(f.condenser), before, "Valid target resumes normal conversion");
    }
    helper.succeed();
  }

  @GameTest
  public void inputConsumptionAndMk2TickOrderMatchForge(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.GOLD_INGOT));
      f.condenser.getInput().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
      BigInteger before = total(f.condenser);
      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getInput(), Items.DIAMOND), mk2 ? 0 : 2, "Forge input rate");
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), mk2 ? 0 : 1, "Forge first tick output");
      helper.assertValueEqual(f.condenser.getStoredEmc(), mk2 ? 24576L : 6144L, "Forge first tick remainder");
      helper.assertValueEqual(total(f.condenser), before, "First tick conserves EMC");
      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), mk2 ? 12 : 2, "Forge second tick output");
      helper.assertValueEqual(total(f.condenser), before, "Second tick conserves EMC");
    }
    helper.succeed();
  }

  @GameTest
  public void shiftClicksAndAutomationTransferRealItemsOnly(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.GOLD_INGOT));
      f.condenser.getInput().setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
      f.player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 3));
      int playerSlot = f.condenser.getInput().getSlots() + (mk2 ? f.condenser.getOutput().getSlots() : 0) + 1;
      f.menu.clicked(playerSlot, 0, ContainerInput.QUICK_MOVE, f.player);
      helper.assertValueEqual(count(f.condenser.getInput(), Items.DIAMOND), 4, "Shift-in moves exact count");
      helper.assertTrue(f.player.getInventory().getItem(9).isEmpty(), "Transferred player stack is removed");
      BigInteger before = total(f.condenser);
      for (int i = 0; i < 16; i++) {
        tick(f.condenser);
      }
      helper.assertValueEqual(total(f.condenser), before, "Shift-in conversion conserves EMC");
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), 16, "Production before shift-out");
      int outputSlot = -1;
      for (int slot = 1; slot < playerSlot; slot++) {
        if (f.menu.getSlot(slot).getItem().is(Items.GOLD_INGOT)) {
          outputSlot = slot;
          break;
        }
      }
      helper.assertTrue(outputSlot > 0, "Find the actual generated output slot");
      f.menu.clicked(outputSlot, 0, ContainerInput.QUICK_MOVE, f.player);
      helper.assertValueEqual(count(f.player.getInventory(), Items.GOLD_INGOT), 16, "Shift-out exact production count");
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), 0, "Output removed once");
      f.menu.clicked(outputSlot, 0, ContainerInput.QUICK_MOVE, f.player);
      helper.assertValueEqual(count(f.player.getInventory(), Items.GOLD_INGOT), 16, "Repeated shift has no new items");

      IItemHandler automation = CondenserBlockEntity.INVENTORY_PROVIDER.getCapability(f.condenser, null);
      ItemStack target = new ItemStack(Items.GOLD_INGOT);
      helper.assertValueEqual(automation.insertItem(0, target, false).getCount(), 1, "Automation rejects the target as input");
      helper.assertTrue(automation.insertItem(0, new ItemStack(Items.DIAMOND), true).isEmpty(), "Automation insertion simulation");
      helper.assertTrue(f.condenser.getInput().getStackInSlot(0).isEmpty(), "Simulation leaves input empty");
      helper.assertTrue(automation.insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty(), "Automation input");
      helper.assertTrue(automation.extractItem(0, 1, false).isEmpty(), "Unsold input cannot be extracted by automation");
    }
    helper.succeed();
  }

  @GameTest
  public void fullOutputAndDifferentComponentsDoNotChargeForMissingItems(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(value(Items.DIAMOND), EmcAction.EXECUTE);
      for (int slot = 0; slot < f.condenser.getOutput().getSlots(); slot++) {
        f.condenser.getOutput().setStackInSlot(slot, new ItemStack(Items.DIAMOND, 64));
      }
      tick(f.condenser);
      helper.assertValueEqual(f.condenser.getStoredEmc(), value(Items.DIAMOND), "Full output is not charged");
      ItemStack named = new ItemStack(Items.DIAMOND, 1);
      named.set(DataComponents.CUSTOM_NAME, Component.literal("Do not merge with generated output"));
      f.condenser.getOutput().setStackInSlot(0, named);
      BigInteger before = total(f.condenser);
      tick(f.condenser);
      helper.assertValueEqual(f.condenser.getStoredEmc(), value(Items.DIAMOND), "Incompatible partial stack is not space");
      helper.assertValueEqual(f.condenser.getOutput().getStackInSlot(0).getCount(), 1, "Existing components/count retained");
      helper.assertValueEqual(total(f.condenser), before, "No EMC discarded by incompatible output");
      f.condenser.getOutput().setStackInSlot(1, ItemStack.EMPTY);
      tick(f.condenser);
      helper.assertValueEqual(f.condenser.getStoredEmc(), 0L, "An empty output slot allows one payment");
      helper.assertValueEqual(f.condenser.getOutput().getStackInSlot(1).getCount(), 1, "Exactly one plain output");
      helper.assertTrue(f.condenser.getOutput().getStackInSlot(1).get(DataComponents.CUSTOM_NAME) == null, "No output component leakage");
    }
    helper.succeed();
  }

  @GameTest
  public void savedDataReloadOnExistingEntityInvalidatesTargetCost(GameTestHelper helper) throws IOException {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(value(Items.DIAMOND) + 913, EmcAction.EXECUTE);
      tick(f.condenser);
      select(helper, f, new ItemStack(Items.DIAMOND));
      CompoundTag saved = binaryRoundTrip(f.condenser.saveWithFullMetadata(f.condenser.getLevel().registryAccess()));
      BigInteger before = total(f.condenser);

      select(helper, f, new ItemStack(Items.GOLD_INGOT));
      f.condenser.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, f.condenser.getLevel().registryAccess(), saved));
      helper.assertValueEqual(f.condenser.requiredEmc, value(Items.DIAMOND), "Loaded target immediately restores its own cost");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 913L, "Loaded EMC balance");
      helper.assertValueEqual(total(f.condenser), before, "Saved inventory and EMC round-trip");
      tick(f.condenser);
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 1, "Reload cannot use the previous low cost");
      helper.assertValueEqual(total(f.condenser), before, "Reload tick conserves EMC");

      CondenserBlockEntity fresh = (CondenserBlockEntity) BlockEntity.loadStatic(f.condenser.getBlockPos(), f.condenser.getBlockState(), saved, f.condenser.getLevel().registryAccess());
      helper.assertTrue(fresh != null, "Saved block entity can be reconstructed");
      fresh.setLevel(f.condenser.getLevel());
      tick(fresh);
      helper.assertValueEqual(fresh.requiredEmc, value(Items.DIAMOND), "Fresh chunk-load cost");
      helper.assertValueEqual(total(fresh), before, "Fresh block entity preserves save format");
    }
    helper.succeed();
  }

  @GameTest
  public void loadingEmptyDataDoesNotRetainPreviousInventoryOrTarget(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(501, EmcAction.EXECUTE);
      f.condenser.getInput().setStackInSlot(0, new ItemStack(Items.COAL, 7));
      f.condenser.getOutput().setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, 3));
      f.condenser.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, f.condenser.getLevel().registryAccess(), new CompoundTag()));
      helper.assertValueEqual(total(f.condenser), BigInteger.ZERO, "Empty data cannot retain old items/EMC");
      helper.assertTrue(f.condenser.getLockInfo() == null, "Empty data resets lock");
      helper.assertValueEqual(f.condenser.requiredEmc, 0L, "Empty data resets cached price");
      helper.assertValueEqual(f.condenser.insertEmc(1, EmcAction.EXECUTE), 0L, "Empty data resets acceptance");
      tick(f.condenser);
      helper.assertValueEqual(total(f.condenser), BigInteger.ZERO, "No free output after empty load");
    }
    helper.succeed();
  }

  @GameTest(maxTicks = 400)
  public void chunkUnloadAndDiskReloadKeepPaidOutputAndEmc(GameTestHelper helper) {
    var level = helper.getLevel();
    var cache = level.getChunkSource();
    ChunkPos origin = ChunkPos.containing(helper.absolutePos(POSITION));
    ChunkPos remote = new ChunkPos(origin.x() + 128, origin.z() + 128);
    level.setChunkForced(remote.x(), remote.z(), true);
    level.getChunk(remote.x(), remote.z());
    List<Fixture> fixtures = new ArrayList<>();
    List<BigInteger> totals = new ArrayList<>();
    for (boolean mk2 : variants()) {
      BlockPos position = remote.getBlockAt(mk2 ? 5 : 2, level.getMinY() + 5, 2);
      Fixture f = fixtureAt(helper, mk2, helper.makeMockPlayer(GameType.SURVIVAL), position);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(value(Items.DIAMOND) + 431, EmcAction.EXECUTE);
      tick(f.condenser);
      f.menu.setCarried(ItemStack.EMPTY);
      f.menu.removed(f.player);
      f.player.containerMenu = f.player.inventoryMenu;
      fixtures.add(f);
      totals.add(total(f.condenser));
    }

    cache.save(true);
    cache.chunkMap.synchronize(true).join();
    var persisted = cache.chunkMap.read(remote);
    level.setChunkForced(remote.x(), remote.z(), false);
    helper.startSequence()
        .thenWaitUntil(() -> {
          helper.assertTrue(persisted.isDone(), "Disk read completes");
          helper.assertTrue(persisted.join().isPresent(), "Condenser chunk was actually saved to disk");
          helper.assertTrue(cache.chunkMap.getUpdatingChunkIfPresent(remote.pack()) == null, "Remote chunk is fully unloaded");
          for (Fixture f : fixtures) {
            helper.assertTrue(f.condenser.isRemoved(), "Old block entity was unloaded");
          }
        })
        .thenExecute(() -> {
          level.getChunk(remote.x(), remote.z());
          for (int index = 0; index < fixtures.size(); index++) {
            CondenserBlockEntity old = fixtures.get(index).condenser;
            CondenserBlockEntity loaded = (CondenserBlockEntity) level.getBlockEntity(old.getBlockPos());
            helper.assertTrue(loaded != null && loaded != old, "Disk reload creates a new block entity");
            tick(loaded);
            helper.assertValueEqual(loaded.requiredEmc, value(Items.DIAMOND), "Reloaded target cost");
            helper.assertValueEqual(loaded.getStoredEmc(), 431L, "Reloaded exact EMC balance");
            helper.assertValueEqual(count(loaded.getOutput(), Items.DIAMOND), 1, "Reloaded paid output count");
            helper.assertValueEqual(total(loaded), totals.get(index), "Chunk lifecycle conserves item value and balance");
          }
        })
        .thenSucceed();
  }

  @GameTest
  public void reopeningMenuResendsGhostAndEmcWithoutChangingServerState(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND, 9));
      f.condenser.insertEmc(733, EmcAction.EXECUTE);
      tick(f.condenser);
      f.menu.setCarried(ItemStack.EMPTY);
      f.menu.removed(f.player);
      List<IPEPacket> packets = new ArrayList<>();
      CondenserContainer reopened = recordingMenu(22, f.player.getInventory(), f.condenser, packets);
      f.player.containerMenu = reopened;
      reopened.sendAllDataToRemote();
      helper.assertValueEqual(reopened.requiredEmc.get(), value(Items.DIAMOND), "Reopened target cost");
      helper.assertValueEqual(reopened.displayEmc.get(), 733L, "Reopened EMC display");
      helper.assertValueEqual(reopened.getSlot(0).getItem().getCount(), 1, "Ghost display count is one");
      helper.assertTrue(packets.stream().anyMatch(p -> p instanceof UpdateCondenserLockPKT lock && lock.windowId() == 22 && ItemInfo.fromItem(Items.DIAMOND).equals(lock.lockInfo())), "Initial synchronization includes current target");
      helper.assertTrue(packets.stream().anyMatch(p -> p instanceof UpdateWindowLongPKT value && value.propVal() == 733), "Initial synchronization includes stored EMC");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 733L, "Reopen never spends/refunds EMC");
    }
    helper.succeed();
  }

  @GameTest
  public void clientboundPacketsCannotRewriteServerTargetAndCost(GameTestHelper helper) {
    helper.assertFalse(ServerPlayNetworking.getGlobalReceivers().contains(UpdateCondenserLockPKT.TYPE.id()), "Target sync has no serverbound receiver");
    helper.assertFalse(ServerPlayNetworking.getGlobalReceivers().contains(UpdateWindowLongPKT.TYPE.id()), "EMC display sync has no serverbound receiver");
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      select(helper, f, new ItemStack(Items.DIAMOND));
      f.condenser.insertEmc(739, EmcAction.EXECUTE);
      for (int i = 0; i < 20; i++) {
        new UpdateCondenserLockPKT((short) f.menu.containerId, ItemInfo.fromItem(Items.GOLD_INGOT)).handle(() -> f.player);
        f.condenser.setLockInfoFromPacket(null);
        new UpdateWindowLongPKT((short) f.menu.containerId, (short) 1, 1).handle(() -> f.player);
      }
      tick(f.condenser);
      f.menu.broadcastChanges();
      helper.assertValueEqual(f.condenser.getLockInfo(), ItemInfo.fromItem(Items.DIAMOND), "Server target is authoritative");
      helper.assertValueEqual(f.condenser.requiredEmc, value(Items.DIAMOND), "Server price is authoritative");
      helper.assertValueEqual(f.menu.requiredEmc.get(), value(Items.DIAMOND), "Server resynchronizes display from real price");
      helper.assertValueEqual(f.condenser.getStoredEmc(), 739L, "Repeated sync cannot fund output");
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), 0, "No injected target output");
    }
    helper.succeed();
  }

  @GameTest
  public void wrongOrOutOfRangeMenusCannotSetServerTarget(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      f.menu.setCarried(new ItemStack(Items.DIAMOND, 13));
      f.player.containerMenu = f.player.inventoryMenu;
      helper.assertFalse(f.condenser.attemptCondenserSet(f.player), "Another menu cannot set the target");
      f.player.containerMenu = f.menu;
      BlockPos position = f.condenser.getBlockPos();
      f.player.setPos(position.getX() + 100, position.getY(), position.getZ());
      helper.assertFalse(f.condenser.attemptCondenserSet(f.player), "Out-of-range target set is rejected");
      helper.assertTrue(f.condenser.getLockInfo() == null, "Invalid menus leave the target unset");
      helper.assertValueEqual(f.menu.getCarried().getCount(), 13, "Rejected set preserves the cursor");
    }
    helper.succeed();
  }

  @GameTest
  public void canceledSetEventPreservesForgeGhostBehavior(GameTestHelper helper) {
    for (boolean mk2 : variants()) {
      Fixture f = fixture(helper, mk2);
      AtomicBoolean cancel = new AtomicBoolean(true);
      NeoForge.EVENT_BUS.addListener(PlayerAttemptCondenserSetEvent.class, event -> {
        if (event.getPlayer() == f.player && cancel.get()) {
          event.setCanceled(true);
        }
      });
      f.menu.setCarried(new ItemStack(Items.DIAMOND, 15));
      f.menu.clicked(0, 0, ContainerInput.PICKUP, f.player);
      helper.assertTrue(f.condenser.getLockInfo() == null, "Cancellation prevents target set");
      helper.assertValueEqual(f.menu.getCarried().getCount(), 15, "Cancellation cannot consume source items");
      helper.assertValueEqual(f.condenser.requiredEmc, 0L, "Cancellation cannot leave a stale cost");
      cancel.set(false);
      select(helper, f, new ItemStack(Items.DIAMOND, 15));
      helper.assertValueEqual(f.condenser.requiredEmc, value(Items.DIAMOND), "Normal set still works after cancellation");
    }
    helper.succeed();
  }

  @GameTest
  public void exactComponentTargetsKeepTheirOwnEmcAndStackLimit(GameTestHelper helper) {
    var original = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    ItemStack componentTarget = new ItemStack(Items.DIAMOND, 16);
    componentTarget.set(DataComponents.CUSTOM_NAME, Component.literal("Custom mapped target"));
    componentTarget.set(DataComponents.MAX_STACK_SIZE, 16);
    ItemInfo info = ItemInfo.fromStack(componentTarget);
    var custom = new Object2LongOpenHashMap<ItemInfo>(original);
    custom.put(info, 12001L);
    EMCMappingHandler.updateEmcValues(custom);
    try {
      for (boolean mk2 : variants()) {
        Fixture f = fixture(helper, mk2);
        select(helper, f, componentTarget.copy());
        helper.assertValueEqual(f.condenser.getLockInfo(), info, "Exact EMC mapping retains persistent components");
        helper.assertValueEqual(f.condenser.requiredEmc, 12001L, "Component target's own value");
        f.condenser.getOutput().setStackInSlot(0, componentTarget.copyWithCount(15));
        f.condenser.insertEmc(12001L, EmcAction.EXECUTE);
        BigInteger before = total(f.condenser);
        tick(f.condenser);
        helper.assertValueEqual(f.condenser.getOutput().getStackInSlot(0).getCount(), 16, "Custom stack limit is honored");
        helper.assertValueEqual(f.condenser.getOutput().getStackInSlot(0).getComponents(), componentTarget.getComponents(), "Output components are preserved");
        helper.assertValueEqual(f.condenser.getStoredEmc(), 0L, "Custom EMC cost is paid once");
        helper.assertValueEqual(total(f.condenser), before, "Component-sensitive output conserves EMC");
      }
    } finally {
      EMCMappingHandler.updateEmcValues(original);
    }
    helper.succeed();
  }

  @GameTest
  public void failedOutputInsertionRefundsExactlyOnePayment(GameTestHelper helper) {
    Fixture placed = fixture(helper, false);
    CondenserBlockEntity rejecting = new CondenserBlockEntity(placed.condenser.getBlockPos(), placed.condenser.getBlockState()) {
      @Override
      protected ItemStackHandler createOutput() {
        return new ItemStackHandler(1) {
          @Override
          public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return simulate ? ItemStack.EMPTY : stack;
          }
        };
      }
    };
    helper.getLevel().setBlockEntity(rejecting);
    Fixture f = attachMenu(placed.player, rejecting);
    select(helper, f, new ItemStack(Items.DIAMOND));
    f.condenser.insertEmc(value(Items.DIAMOND) + 13, EmcAction.EXECUTE);
    for (int i = 0; i < 10; i++) {
      tick(f.condenser);
      helper.assertValueEqual(f.condenser.getStoredEmc(), value(Items.DIAMOND) + 13, "Failed insertion refunds only the reserved payment");
      helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 0, "Rejected output cannot appear");
    }
    helper.succeed();
  }

  @GameTest
  @SuppressWarnings("removal")
  public void actualRepeatedAndStaleClickPacketsConserveEmc(GameTestHelper helper) {
    ServerPlayer player = helper.makeMockServerPlayerInLevel();
    try {
      for (boolean mk2 : variants()) {
        Fixture f = fixture(helper, mk2, player);
        select(helper, f, new ItemStack(Items.DIAMOND));
        f.condenser.insertEmc(719, EmcAction.EXECUTE);
        f.menu.setCarried(new ItemStack(Items.GOLD_INGOT, 17));
        ServerboundContainerClickPacket repeated = new ServerboundContainerClickPacket(
            f.menu.containerId, f.menu.getStateId(), (short) 0, (byte) 0, ContainerInput.PICKUP,
            new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY);
        for (int i = 0; i < 40; i++) {
          player.connection.handleContainerClick(repeated);
          helper.assertValueEqual(f.condenser.getStoredEmc(), 719L, "Replayed click cannot charge/refund EMC");
          if (f.condenser.getLockInfo() != null) {
            helper.assertValueEqual(f.condenser.requiredEmc, value(Items.GOLD_INGOT), "Replayed set uses server cursor price");
          }
        }
        helper.assertValueEqual(f.menu.getCarried().getCount(), 17, "Client cursor hash cannot replace server cursor");
        player.connection.handleContainerClick(new ServerboundContainerClickPacket(
            f.menu.containerId + 1, f.menu.getStateId(), (short) 0, (byte) 0, ContainerInput.PICKUP,
            new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY));
        tick(f.condenser);
        helper.assertValueEqual(f.condenser.getStoredEmc(), 719L, "Old window packet cannot create output");
        helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), 0, "Insufficient EMC under replay");
      }
    } finally {
      player.discard();
      player.level().getServer().getPlayerList().remove(player);
    }
    helper.succeed();
  }

  @GameTest
  public void changedEmcMapCannotPurchaseOutputAtStalePrice(GameTestHelper helper) {
    var original = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      for (boolean mk2 : variants()) {
        EMCMappingHandler.updateEmcValues(original);
        Fixture f = fixture(helper, mk2);
        select(helper, f, new ItemStack(Items.DIAMOND));
        f.condenser.insertEmc(value(Items.DIAMOND), EmcAction.EXECUTE);
        var changed = new Object2LongOpenHashMap<ItemInfo>(original);
        changed.put(ItemInfo.fromItem(Items.DIAMOND), 16384L);
        EMCMappingHandler.updateEmcValues(changed);
        tick(f.condenser);
        helper.assertValueEqual(count(f.condenser.getOutput(), Items.DIAMOND), 0, "Output is validated against current map");
        helper.assertValueEqual(f.condenser.getStoredEmc(), 8192L, "Rejected stale price costs nothing");
        helper.assertValueEqual(f.condenser.requiredEmc, 16384L, "Stale cache is refreshed");
      }
    } finally {
      EMCMappingHandler.updateEmcValues(original);
    }
    helper.succeed();
  }

  @GameTest(maxTicks = 200)
  public void valuelessLockSurvivesDataPackReloadAndRegainsItsEmc(GameTestHelper helper) {
    var original = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    var valueless = new Object2LongOpenHashMap<ItemInfo>(original);
    valueless.put(ItemInfo.fromItem(Items.DIAMOND), 0L);
    List<Fixture> fixtures = new ArrayList<>();
    try {
      for (boolean mk2 : variants()) {
        EMCMappingHandler.updateEmcValues(original);
        BlockPos position = helper.absolutePos(POSITION).offset(mk2 ? 3 : 0, 0, 0);
        Fixture f = fixtureAt(helper, mk2, helper.makeMockPlayer(GameType.SURVIVAL), position);
        select(helper, f, new ItemStack(Items.DIAMOND));
        f.condenser.insertEmc(431, EmcAction.EXECUTE);
        clear(f);
        EMCMappingHandler.updateEmcValues(valueless);
        f.menu.setCarried(new ItemStack(Items.DIAMOND));
        f.menu.clicked(0, 0, ContainerInput.PICKUP, f.player);
        helper.assertTrue(f.condenser.getLockInfo() == null, "Zero-valued lock is hidden");
        helper.assertTrue(
            f.condenser.saveWithoutMetadata(helper.getLevel().registryAccess()).contains("lock"),
            "Valueless lock is still saved for a later remap, like Forge");
        fixtures.add(f);
      }
    } finally {
      EMCMappingHandler.updateEmcValues(original);
    }

    int loadIndex = EMCMappingHandler.getLoadIndex();
    var server = helper.getLevel().getServer();
    var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
    helper.startSequence()
        .thenWaitUntil(() -> helper.assertTrue(reload.isDone(), "Data pack reload completes"))
        .thenExecute(() -> {
          reload.join();
          helper.assertTrue(EMCMappingHandler.getLoadIndex() != loadIndex, "Real EMC remap increments the load index");
          for (Fixture f : fixtures) {
            tick(f.condenser);
            helper.assertValueEqual(f.condenser.getLockInfo(), ItemInfo.fromItem(Items.DIAMOND), "Previously valueless lock reappears");
            helper.assertValueEqual(f.condenser.requiredEmc, 8192L, "Remap restores the correct EMC cost");
            helper.assertValueEqual(f.condenser.getStoredEmc(), 431L, "Remap preserves accumulated EMC");
            helper.assertValueEqual(f.condenser.insertEmc(1, EmcAction.SIMULATE), 1L, "Restored lock accepts EMC again");
          }
        })
        .thenSucceed();
  }

  @GameTest
  public void hugeInputStackCannotOverflowOrDestroyUnstoredEmc(GameTestHelper helper) {
    Fixture f = fixture(helper, true);
    select(helper, f, new ItemStack(Items.NETHER_STAR));
    f.condenser.getInput().setStackInSlot(0, new ItemStack(Items.BARRIER, 64));
    BigInteger before = total(f.condenser);
    tick(f.condenser);
    helper.assertValueEqual(count(f.condenser.getInput(), Items.BARRIER), 63, "Only a storable input item is sold");
    helper.assertValueEqual(f.condenser.getStoredEmc(), value(Items.BARRIER), "No multiplication overflow");
    helper.assertValueEqual(total(f.condenser), before, "Huge stack EMC remains in unsold inputs");
    tick(f.condenser);
    helper.assertValueEqual(count(f.condenser.getInput(), Items.BARRIER), 63, "Overflowing second input is retained");
    helper.assertValueEqual(total(f.condenser), before, "No lost EMC when storage cannot fit another item");
    helper.succeed();
  }

  @GameTest
  public void covalenceLossIsAppliedOnlyToActuallyConsumedInputs(GameTestHelper helper) {
    double original = ProjectEConfig.server.difficulty.covalenceLoss.get();
    ProjectEConfig.server.difficulty.covalenceLoss.set(0.5);
    try {
      for (boolean mk2 : variants()) {
        Fixture f = fixture(helper, mk2);
        select(helper, f, new ItemStack(Items.GOLD_INGOT));
        f.condenser.getInput().setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        tick(f.condenser);
        helper.assertValueEqual(count(f.condenser.getInput(), Items.DIAMOND), mk2 ? 0 : 1, "Real consumed input count");
        helper.assertValueEqual(f.condenser.getStoredEmc(), mk2 ? 8192L : 2048L, "Forge sell loss and full output cost");
        helper.assertValueEqual(count(f.condenser.getOutput(), Items.GOLD_INGOT), mk2 ? 0 : 1, "No loss applied twice to output");
      }
    } finally {
      ProjectEConfig.server.difficulty.covalenceLoss.set(original);
    }
    helper.succeed();
  }

  @GameTest
  public void lockPacketCodecKeepsComponentsAndOneItemGhost(GameTestHelper helper) {
    ItemStack source = new ItemStack(Items.DIAMOND, 64);
    source.set(DataComponents.CUSTOM_NAME, Component.literal("Component-sensitive target"));
    ItemInfo info = ItemInfo.fromStack(source);
    RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
    try {
      UpdateCondenserLockPKT.STREAM_CODEC.encode(buffer, new UpdateCondenserLockPKT((short) 23, info));
      UpdateCondenserLockPKT decoded = UpdateCondenserLockPKT.STREAM_CODEC.decode(buffer);
      helper.assertValueEqual(decoded.lockInfo(), info, "Lock components survive synchronization");
      helper.assertValueEqual(decoded.lockInfo().createStack().getCount(), 1, "Source count is not cached in the ghost");
      helper.assertValueEqual(source.getCount(), 64, "Source stack is not modified");
      UpdateCondenserLockPKT.STREAM_CODEC.encode(buffer, new UpdateCondenserLockPKT((short) 23, null));
      helper.assertTrue(UpdateCondenserLockPKT.STREAM_CODEC.decode(buffer).lockInfo() == null, "Clear packet round-trips");
    } finally {
      buffer.release();
    }
    helper.succeed();
  }

  private static boolean[] variants() {
    return new boolean[] {false, true};
  }

  private static Fixture fixture(GameTestHelper helper, boolean mk2) {
    return fixture(helper, mk2, helper.makeMockPlayer(GameType.SURVIVAL));
  }

  private static Fixture fixture(GameTestHelper helper, boolean mk2, Player player) {
    return fixtureAt(helper, mk2, player, helper.absolutePos(POSITION));
  }

  private static Fixture fixtureAt(GameTestHelper helper, boolean mk2, Player player, BlockPos position) {
    helper.getLevel().setBlockAndUpdate(position, mk2 ? PEBlocks.CONDENSER_MK2.defaultState() : PEBlocks.CONDENSER.defaultState());
    CondenserBlockEntity condenser = (CondenserBlockEntity) helper.getLevel().getBlockEntity(position);
    helper.assertTrue(condenser != null, "Condenser block entity exists");
    return attachMenu(player, condenser);
  }

  private static Fixture attachMenu(Player player, CondenserBlockEntity condenser) {
    Vec3 position = Vec3.atCenterOf(condenser.getBlockPos());
    player.setPos(position.x, position.y, position.z + 2);
    List<IPEPacket> packets = new ArrayList<>();
    CondenserContainer menu = recordingMenu(17, player.getInventory(), condenser, packets);
    player.containerMenu = menu;
    return new Fixture(condenser, player, menu, packets);
  }

  private static CondenserContainer recordingMenu(int id, Inventory inventory, CondenserBlockEntity condenser, List<IPEPacket> packets) {
    if (condenser instanceof CondenserMK2BlockEntity mk2) {
      return new CondenserMK2Container(id, inventory, mk2) {
        @Override
        protected void syncDataChange(IPEPacket packet) {
          packets.add(packet);
        }
      };
    }
    return new CondenserContainer(id, inventory, condenser) {
      @Override
      protected void syncDataChange(IPEPacket packet) {
        packets.add(packet);
      }
    };
  }

  private static void select(GameTestHelper helper, Fixture fixture, ItemStack source) {
    clear(fixture);
    long emc = fixture.condenser.getStoredEmc();
    fixture.menu.setCarried(source);
    fixture.menu.clicked(0, 0, ContainerInput.PICKUP, fixture.player);
    helper.assertTrue(fixture.condenser.getLockInfo() != null, "Server accepts valued target");
    helper.assertValueEqual(fixture.menu.getSlot(0).getItem().getCount(), 1, "Ghost count ignores cursor count");
    helper.assertValueEqual(fixture.menu.getCarried().getCount(), source.getCount(), "Setting target preserves cursor count");
    helper.assertValueEqual(fixture.condenser.getStoredEmc(), emc, "Changing target preserves stored EMC");
  }

  private static void clear(Fixture fixture) {
    if (fixture.condenser.getLockInfo() != null) {
      fixture.menu.setCarried(ItemStack.EMPTY);
      fixture.menu.clicked(0, 0, ContainerInput.PICKUP, fixture.player);
    }
  }

  private static void tick(CondenserBlockEntity condenser) {
    CondenserBlockEntity.tickServer(condenser.getLevel(), condenser.getBlockPos(), condenser.getBlockState(), condenser);
  }

  private static long value(Item item) {
    return IEMCProxy.INSTANCE.getValue(item);
  }

  private static BigInteger total(CondenserBlockEntity condenser) {
    BigInteger emc = BigInteger.valueOf(condenser.getStoredEmc()).add(inventoryValue(condenser.getInput()));
    return condenser.getInput() == condenser.getOutput() ? emc : emc.add(inventoryValue(condenser.getOutput()));
  }

  private static BigInteger inventoryValue(IItemHandler inventory) {
    BigInteger emc = BigInteger.ZERO;
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      ItemStack stack = inventory.getStackInSlot(slot);
      emc = emc.add(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(stack)).multiply(BigInteger.valueOf(stack.getCount())));
    }
    return emc;
  }

  private static int count(IItemHandler inventory, Item item) {
    int count = 0;
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      ItemStack stack = inventory.getStackInSlot(slot);
      if (stack.is(item)) {
        count += stack.getCount();
      }
    }
    return count;
  }

  private static int count(Inventory inventory, Item item) {
    int count = 0;
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (stack.is(item)) {
        count += stack.getCount();
      }
    }
    return count;
  }

  private static void clearInventory(ItemStackHandler inventory) {
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      inventory.setStackInSlot(slot, ItemStack.EMPTY);
    }
  }

  private static CompoundTag binaryRoundTrip(CompoundTag tag) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    NbtIo.writeCompressed(tag, output);
    return NbtIo.readCompressed(new ByteArrayInputStream(output.toByteArray()), NbtAccounter.unlimitedHeap());
  }

  private record Fixture(CondenserBlockEntity condenser, Player player, CondenserContainer menu, List<IPEPacket> packets) {}
}
