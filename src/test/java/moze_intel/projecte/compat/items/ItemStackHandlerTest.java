package moze_intel.projecte.compat.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemStackHandlerTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    // In 26.2, registry component defaults are bound separately from registry bootstrap.
    BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
        .build(VanillaRegistries.createLookup())
        .forEach(DataComponentInitializers.PendingComponents::apply);
  }

  @Test
  void oneGeneratedItemOnlyAddsOneToExistingOutput() {
    ItemStackHandler output = new ItemStackHandler(1);
    output.setStackInSlot(0, new ItemStack(Items.DIAMOND));
    ItemStack generated = new ItemStack(Items.DIAMOND);

    assertTrue(ItemHandlerHelper.insertItemStacked(output, generated, true).isEmpty());
    assertEquals(1, output.getStackInSlot(0).getCount(), "Simulation must not generate items");
    assertTrue(ItemHandlerHelper.insertItemStacked(output, generated, false).isEmpty());
    assertEquals(2, output.getStackInSlot(0).getCount(), "One EMC payment must add just one item");
    assertEquals(1, generated.getCount(), "Insertion must not modify the supplied stack");
  }

  @Test
  void insertionConservesCountsAndMatchesSimulation() {
    for (int maximum : new int[] {16, 64}) {
      for (int existingCount : new int[] {0, 1, maximum / 2, maximum - 1, maximum}) {
        for (int incomingCount : new int[] {1, 2, 10, 64}) {
          ItemStackHandler inventory = new ItemStackHandler(1);
          ItemStack incoming = new ItemStack(Items.DIAMOND, incomingCount);
          incoming.set(DataComponents.MAX_STACK_SIZE, maximum);
          if (existingCount > 0) {
            inventory.setStackInSlot(0, incoming.copyWithCount(existingCount));
          }

          int accepted = Math.min(incomingCount, maximum - existingCount);
          ItemStack simulated = inventory.insertItem(0, incoming, true);
          assertEquals(incomingCount - accepted, simulated.getCount());
          assertEquals(existingCount, inventory.getStackInSlot(0).getCount());

          ItemStack remainder = inventory.insertItem(0, incoming, false);
          assertEquals(simulated.getCount(), remainder.getCount());
          assertEquals(existingCount + accepted, inventory.getStackInSlot(0).getCount());
          assertEquals(
              existingCount + incomingCount,
              inventory.getStackInSlot(0).getCount() + remainder.getCount(),
              "Stored items plus remainder must equal all supplied items");
          assertEquals(incomingCount, incoming.getCount());
        }
      }
    }
  }

  @Test
  void stackedInsertionKeepsDifferentComponentsSeparate() {
    ItemStackHandler inventory = new ItemStackHandler(2);
    ItemStack named = new ItemStack(Items.DIAMOND, 7);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Existing output"));
    inventory.setStackInSlot(0, named);

    ItemStack incoming = new ItemStack(Items.DIAMOND, 3);
    assertFalse(ItemHandlerHelper.canItemStacksStack(named, incoming));
    assertTrue(ItemHandlerHelper.insertItemStacked(inventory, incoming, false).isEmpty());
    assertEquals(7, inventory.getStackInSlot(0).getCount());
    assertEquals(named.getComponents(), inventory.getStackInSlot(0).getComponents());
    assertEquals(3, inventory.getStackInSlot(1).getCount());
    assertEquals(incoming.getComponents(), inventory.getStackInSlot(1).getComponents());
  }

  @Test
  void invalidInputIsRejectedWithoutMutatingInventory() {
    ItemStackHandler inventory = new ItemStackHandler(1) {
      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(Items.DIAMOND);
      }
    };
    ItemStack invalid = new ItemStack(Items.DIRT, 12);

    assertSame(invalid, inventory.insertItem(0, invalid, true));
    assertSame(invalid, inventory.insertItem(0, invalid, false));
    assertTrue(inventory.getStackInSlot(0).isEmpty());
    assertEquals(12, invalid.getCount());
  }

  @Test
  void nonStackableOutputAndFullSlotsDoNotCreateExtraItems() {
    ItemStackHandler inventory = new ItemStackHandler(2);
    ItemStack sword = new ItemStack(Items.IRON_SWORD);
    assertTrue(ItemHandlerHelper.insertItemStacked(inventory, sword, false).isEmpty());
    assertTrue(ItemHandlerHelper.insertItemStacked(inventory, sword, false).isEmpty());
    assertEquals(1, inventory.getStackInSlot(0).getCount());
    assertEquals(1, inventory.getStackInSlot(1).getCount());
    assertSame(sword, ItemHandlerHelper.insertItemStacked(inventory, sword, false));
  }
}
