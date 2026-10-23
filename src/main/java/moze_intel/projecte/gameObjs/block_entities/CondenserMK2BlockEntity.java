package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.gameObjs.container.CondenserMK2Container;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

public class CondenserMK2BlockEntity extends CondenserBlockEntity {
  public CondenserMK2BlockEntity(BlockPos pos, BlockState state) {
    super(PEBlockEntityTypes.CONDENSER_MK2, pos, state);
  }

  @NotNull
  @Override
  protected IItemHandler createAutomationInventory() {
    IItemHandlerModifiable automationInput =
        new WrappedItemHandler(this.getInput(), WrappedItemHandler.WriteMode.IN) {
          @NotNull
          @Override
          public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return SlotPredicates.HAS_EMC.test(stack)
                    && !CondenserMK2BlockEntity.this.isStackEqualToLock(stack)
                ? super.insertItem(slot, stack, simulate)
                : stack;
          }
        };
    IItemHandlerModifiable automationOutput =
        new WrappedItemHandler(this.getOutput(), WrappedItemHandler.WriteMode.OUT);
    return new CombinedInvWrapper(automationInput, automationOutput);
  }

  @Override
  protected ItemStackHandler createInput() {
    return new EmcBlockEntity.StackHandler(42);
  }

  @Override
  protected ItemStackHandler createOutput() {
    return new EmcBlockEntity.StackHandler(42);
  }

  @Override
  protected void condense() {
    while (this.produceOutput()) {
    }

    if (this.hasSpace()) {
      int i = 0;

      for (int slots = this.getInput().getSlots(); i < slots; i++) {
        ItemStack stack = this.getInput().getStackInSlot(i);
        if (!stack.isEmpty()) {
          this.consumeInput(i, stack.getCount());
          break;
        }
      }
    }
  }

  @Override
  public void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    this.getOutput().deserializeNBT(
        tag.lookup(), tag.read("output", CompoundTag.CODEC).orElseGet(CompoundTag::new));
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    tag.store(
        "output", CompoundTag.CODEC, this.getOutput().serializeNBT(this.level.registryAccess()));
  }

  @Override
  public AbstractContainerMenu createMenu(
      int windowId, @NotNull Inventory playerInv, @NotNull Player player) {
    return new CondenserMK2Container(windowId, playerInv, this);
  }

  @NotNull
  @Override
  public Component getDisplayName() {
    return TextComponentUtil.build(PEBlocks.CONDENSER_MK2);
  }
}
