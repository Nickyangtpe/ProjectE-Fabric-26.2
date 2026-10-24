package moze_intel.projecte.gametest;

import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IPedestalRangeItem;
import moze_intel.projecte.gameObjs.block_entities.DMPedestalBlockEntity;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class PedestalGameTests {
  private static final BlockPos PEDESTAL = new BlockPos(3, 3, 3);

  @GameTest
  public void rangedPedestalItemsExposeTheirExactEffectArea(GameTestHelper helper) {
    helper.setBlock(PEDESTAL, PEBlocks.DARK_MATTER_PEDESTAL.getBlock());
    DMPedestalBlockEntity pedestal =
        helper.getBlockEntity(PEDESTAL, DMPedestalBlockEntity.class);
    Item[] rangedItems = {
        PEItems.REPAIR_TALISMAN.get(),
        PEItems.ARCHANGEL_SMITE.get(),
        PEItems.BLACK_HOLE_BAND.get(),
        PEItems.BODY_STONE.get(),
        PEItems.HARVEST_GODDESS_BAND.get(),
        PEItems.IGNITION_RING.get(),
        PEItems.LIFE_STONE.get(),
        PEItems.MIND_STONE.get(),
        PEItems.SOUL_STONE.get(),
        PEItems.SWIFTWOLF_RENDING_GALE.get(),
        PEItems.WATCH_OF_FLOWING_TIME.get(),
        PEItems.VOID_RING.get(),
        PEItems.ZERO_RING.get()
    };
    AABB expected = pedestal.getEffectBounds();
    for (Item item : rangedItems) {
      ItemStack stack = new ItemStack(item);
      pedestal.getInventory().setStackInSlot(0, stack);
      IPedestalItem pedestalItem =
          PECapabilities.PEDESTAL_ITEM_CAPABILITY.find(stack, null);
      helper.assertTrue(
          pedestalItem instanceof IPedestalRangeItem,
          item.builtInRegistryHolder().getRegisteredName() + " declares a visible range");
      AABB bounds = pedestal.getItemEffectBounds();
      helper.assertValueEqual(bounds, expected, "Preview bounds match the runtime effect bounds");
      helper.assertValueEqual(bounds.getXsize(), 9.0, "Effect width is nine blocks");
      helper.assertValueEqual(bounds.getYsize(), 9.0, "Effect height is nine blocks");
      helper.assertValueEqual(bounds.getZsize(), 9.0, "Effect depth is nine blocks");
    }
    helper.succeed();
  }

  @GameTest
  public void globalAndInvalidPedestalItemsDoNotShowMisleadingRanges(GameTestHelper helper) {
    helper.setBlock(PEDESTAL, PEBlocks.DARK_MATTER_PEDESTAL.getBlock());
    DMPedestalBlockEntity pedestal =
        helper.getBlockEntity(PEDESTAL, DMPedestalBlockEntity.class);
    for (Item item : new Item[] {PEItems.EVERTIDE_AMULET.get(), PEItems.VOLCANITE_AMULET.get()}) {
      ItemStack stack = new ItemStack(item);
      pedestal.getInventory().setStackInSlot(0, stack);
      helper.assertTrue(
          PECapabilities.PEDESTAL_ITEM_CAPABILITY.find(stack, null) instanceof IPedestalItem,
          "Weather item still works on a pedestal");
      helper.assertTrue(
          pedestal.getItemEffectBounds() == null,
          "Dimension-wide weather effects do not claim a local range");
    }
    pedestal.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND));
    helper.assertTrue(
        pedestal.getItemEffectBounds() == null,
        "An unsupported item cannot produce a range preview");
    pedestal.getInventory().setStackInSlot(0, ItemStack.EMPTY);
    helper.assertTrue(
        pedestal.getItemEffectBounds() == null,
        "An empty pedestal cannot produce a range preview");
    helper.succeed();
  }
}
