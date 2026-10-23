package moze_intel.projecte.gameObjs.block_entities;

import java.util.List;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.container.AlchChestContainer;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlchBlockEntityChest extends EmcChestBlockEntity {
  public static final ICapabilityProvider<AlchBlockEntityChest, @Nullable Direction, IItemHandler>
      INVENTORY_PROVIDER = (chest, side) -> chest.inventory;
  private final EmcBlockEntity.StackHandler inventory =
      new EmcBlockEntity.StackHandler(104) {
        @Override
        public void onContentsChanged(int slot) {
          super.onContentsChanged(slot);
          if (AlchBlockEntityChest.this.level != null
              && !AlchBlockEntityChest.this.level.isClientSide()) {
            AlchBlockEntityChest.this.inventoryChanged = true;
          }
        }
      };
  private boolean inventoryChanged;

  public AlchBlockEntityChest(BlockPos pos, BlockState state) {
    super(PEBlockEntityTypes.ALCHEMICAL_CHEST, pos, state, 1000L);
  }

  @Override
  public void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    CompoundTag data =
        tag.read("inventory", CompoundTag.CODEC)
            .orElseGet(
                () -> {
                  CompoundTag legacy = new CompoundTag();
                  legacy.putInt("Size", tag.getIntOr("Size", this.inventory.getSlots()));
                  ListTag items = new ListTag();
                  tag.list("Items", CompoundTag.CODEC)
                      .ifPresent(entries -> entries.stream().forEach(items::add));
                  legacy.put("Items", items);
                  return legacy;
                });
    this.inventory.deserializeNBT(tag.lookup(), data);
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    tag.store(
        "inventory", CompoundTag.CODEC, this.inventory.serializeNBT(this.level.registryAccess()));
  }

  public static void tickClient(
      Level level, BlockPos pos, BlockState state, AlchBlockEntityChest alchChest) {
    int i = 0;

    for (int slots = alchChest.inventory.getSlots(); i < slots; i++) {
      ItemStack stack = alchChest.inventory.getStackInSlot(i);
      IAlchChestItem alchChestItem = PECapabilities.ALCH_CHEST_ITEM_CAPABILITY.find(stack, null);
      if (alchChestItem != null) {
        alchChestItem.updateInAlchChest(level, pos, stack);
      }
    }

    EmcChestBlockEntity.lidAnimateTick(level, pos, state, alchChest);
  }

  public static void tickServer(
      Level level, BlockPos pos, BlockState state, AlchBlockEntityChest alchChest) {
    EmcBlockEntity.StackHandler inventory = alchChest.inventory;
    int i = 0;

    for (int slots = inventory.getSlots(); i < slots; i++) {
      ItemStack stack = inventory.getStackInSlot(i);
      IAlchChestItem alchChestItem = PECapabilities.ALCH_CHEST_ITEM_CAPABILITY.find(stack, null);
      if (alchChestItem != null && alchChestItem.updateInAlchChest(level, pos, stack)) {
        inventory.onContentsChanged(i);
      }
    }

    if (alchChest.inventoryChanged) {
      alchChest.inventoryChanged = false;
      level.sendBlockUpdated(pos, state, state, 2);
    }

    alchChest.updateComparators(level, pos);
  }

  public IItemHandler getInventory() {
    return this.inventory;
  }

  @Override
  protected IItemHandlerModifiable getContainerInventory() {
    return this.inventory;
  }

  @Override
  protected List<IItemHandlerModifiable> getInventoriesForDrops() {
    return List.of(this.inventory);
  }

  @NotNull
  public AbstractContainerMenu createMenu(
      int windowId, @NotNull Inventory playerInventory, @NotNull Player playerIn) {
    return new AlchChestContainer(windowId, playerInventory, this);
  }

  @NotNull
  public Component getDisplayName() {
    return TextComponentUtil.build(PEBlocks.ALCHEMICAL_CHEST);
  }
}
