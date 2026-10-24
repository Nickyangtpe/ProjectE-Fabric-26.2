package moze_intel.projecte.integration.jade;

import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.AlchBlockEntityChest;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity;
import moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.DMPedestalBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.EmcBlockEntity;
import moze_intel.projecte.gameObjs.block_entities.RelayMK1BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class JadeMachineDataProvider implements IServerDataProvider<BlockAccessor> {
  static final JadeMachineDataProvider INSTANCE = new JadeMachineDataProvider();

  private JadeMachineDataProvider() {}

  @Override
  public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
    BlockEntity blockEntity = accessor.getBlockEntity();
    if (!(blockEntity instanceof EmcBlockEntity emcBlockEntity)) {
      return;
    }

    CompoundTag data = new CompoundTag();
    if (blockEntity instanceof DMPedestalBlockEntity pedestal) {
      data.putString("Kind", "pedestal");
      ItemStack stack = pedestal.getInventory().getStackInSlot(0);
      putItem(data, "Item", stack, accessor);
      data.putBoolean("Active", pedestal.getActive());
      data.putInt("Cooldown", pedestal.getActivityCooldown());
      IPedestalItem pedestalItem = PECapabilities.PEDESTAL_ITEM_CAPABILITY.find(stack, null);
      if (pedestalItem != null) {
        List<Component> description = pedestalItem.getPedestalDescription(accessor.tickRate());
        if (!description.isEmpty()) {
          data.store("Description", ComponentSerialization.CODEC.listOf(), description);
        }
      }
      AABB bounds = pedestal.getItemEffectBounds();
      if (bounds != null) {
        data.putInt("RangeX", (int) Math.round(bounds.getXsize()));
        data.putInt("RangeY", (int) Math.round(bounds.getYsize()));
        data.putInt("RangeZ", (int) Math.round(bounds.getZsize()));
      }
    } else if (blockEntity instanceof CondenserBlockEntity condenser) {
      data.putString("Kind", "condenser");
      ItemInfo lockInfo = condenser.getLockInfo();
      if (lockInfo != null) {
        putItem(data, "Target", lockInfo.createStack(), accessor);
      }
      data.putLong("Stored", condenser.getStoredEmc());
      data.putLong("Required", condenser.requiredEmc);
      putItem(data, "Output", firstStack(condenser.getOutput()), accessor);
    } else if (blockEntity instanceof DMFurnaceBlockEntity furnace) {
      data.putString("Kind", "furnace");
      putEmc(data, emcBlockEntity);
      data.putFloat("Progress", furnace.getBurnProgress());
      putItem(data, "Input", firstStack(furnace.getInput()), accessor);
      putItem(data, "Fuel", firstStack(furnace.getFuel()), accessor);
      putItem(data, "Output", firstStack(furnace.getOutput()), accessor);
    } else if (blockEntity instanceof CollectorMK1BlockEntity collector) {
      data.putString("Kind", "collector");
      putEmc(data, emcBlockEntity);
      putItem(data, "Input", firstStack(collector.getInput()), accessor);
      putItem(data, "Upgrading", collector.getAux().getStackInSlot(0), accessor);
      putItem(data, "Output", collector.getAux().getStackInSlot(1), accessor);
      putItem(data, "Target", collector.getAux().getStackInSlot(2), accessor);
    } else if (blockEntity instanceof RelayMK1BlockEntity relay) {
      data.putString("Kind", "relay");
      putEmc(data, emcBlockEntity);
      putItem(data, "Input", firstStack(relay.getInput()), accessor);
      putItem(data, "Charging", relay.getOutput().getStackInSlot(0), accessor);
    } else if (blockEntity instanceof AlchBlockEntityChest chest) {
      data.putString("Kind", "chest");
      putInventorySummary(data, chest.getInventory());
    } else {
      data.putString("Kind", "emc");
      putEmc(data, emcBlockEntity);
    }
    tag.put(PEJadeConstants.MACHINE_DATA, data);
  }

  @Override
  public boolean shouldRequestData(BlockAccessor accessor) {
    return accessor.getBlockEntity() instanceof EmcBlockEntity;
  }

  @Override
  public Identifier getUid() {
    return PEJadeConstants.MACHINE_PROVIDER;
  }

  private static void putEmc(CompoundTag data, EmcBlockEntity blockEntity) {
    data.putLong("Stored", blockEntity.getStoredEmc());
    data.putLong("Capacity", blockEntity.getMaximumEmc());
  }

  private static void putItem(
      CompoundTag data, String key, ItemStack stack, BlockAccessor accessor) {
    if (!stack.isEmpty()) {
      data.put(key, accessor.encodeAsNbt(ItemStack.OPTIONAL_STREAM_CODEC, stack.copy()));
    }
  }

  private static ItemStack firstStack(IItemHandler inventory) {
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      ItemStack stack = inventory.getStackInSlot(slot);
      if (!stack.isEmpty()) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private static void putInventorySummary(CompoundTag data, IItemHandler inventory) {
    int usedSlots = 0;
    int itemCount = 0;
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      ItemStack stack = inventory.getStackInSlot(slot);
      if (!stack.isEmpty()) {
        usedSlots++;
        itemCount += stack.getCount();
      }
    }
    data.putInt("UsedSlots", usedSlots);
    data.putInt("Slots", inventory.getSlots());
    data.putInt("ItemCount", itemCount);
  }
}
