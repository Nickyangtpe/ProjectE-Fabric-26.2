package moze_intel.projecte.gameObjs.block_entities;

import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptCondenserSetEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

public class CondenserBlockEntity extends EmcChestBlockEntity {
  public static final ICapabilityProvider<CondenserBlockEntity, @Nullable Direction, IItemHandler>
      INVENTORY_PROVIDER = (condenser, side) -> condenser.automationInventory;
  protected final ItemStackHandler inputInventory = this.createInput();
  private final ItemStackHandler outputInventory = this.createOutput();
  private final IItemHandlerModifiable containerInventory =
      this.inputInventory == this.outputInventory
          ? this.inputInventory
          : new CombinedInvWrapper(this.inputInventory, this.outputInventory);
  @Nullable private ItemInfo lockInfo;
  private boolean isAcceptingEmc;
  public long displayEmc;
  public long requiredEmc;
  private int loadIndex = EMCMappingHandler.getLoadIndex() - 1;
  private final IItemHandler automationInventory = this.createAutomationInventory();

  public CondenserBlockEntity(BlockPos pos, BlockState state) {
    this(PEBlockEntityTypes.CONDENSER, pos, state);
  }

  protected CondenserBlockEntity(
      BlockEntityTypeRegistryObject<? extends CondenserBlockEntity> type,
      BlockPos pos,
      BlockState state) {
    super(type, pos, state);
  }

  @Override
  protected boolean canAcceptEmc() {
    return this.isAcceptingEmc;
  }

  @Override
  protected boolean canProvideEmc() {
    return false;
  }

  @Nullable
  public final ItemInfo getLockInfo() {
    return this.requiredEmc != 0L || this.level != null && this.level.isClientSide()
        ? this.lockInfo
        : null;
  }

  public ItemStackHandler getInput() {
    return this.inputInventory;
  }

  public ItemStackHandler getOutput() {
    return this.outputInventory;
  }

  @Override
  protected IItemHandlerModifiable getContainerInventory() {
    return this.containerInventory;
  }

  @Override
  protected IItemHandler getAutomationInventory(@Nullable Direction side) {
    return this.automationInventory;
  }

  @Override
  protected List<IItemHandlerModifiable> getInventoriesForDrops() {
    // MK1 shares its input/output inventory. The target is a ghost, not a stored item.
    return this.inputInventory == this.outputInventory
        ? List.of(this.inputInventory)
        : List.of(this.inputInventory, this.outputInventory);
  }

  protected ItemStackHandler createInput() {
    return new EmcBlockEntity.StackHandler(91);
  }

  protected ItemStackHandler createOutput() {
    return this.inputInventory;
  }

  @NotNull
  protected IItemHandler createAutomationInventory() {
    return new WrappedItemHandler(this.inputInventory, WrappedItemHandler.WriteMode.IN_OUT) {
      @NotNull
      @Override
      public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return SlotPredicates.HAS_EMC.test(stack)
                && !CondenserBlockEntity.this.isStackEqualToLock(stack)
            ? super.insertItem(slot, stack, simulate)
            : stack;
      }

      @NotNull
      @Override
      public ItemStack extractItem(int slot, int max, boolean simulate) {
        return CondenserBlockEntity.this.isStackEqualToLock(this.getStackInSlot(slot))
            ? super.extractItem(slot, max, simulate)
            : ItemStack.EMPTY;
      }
    };
  }

  public static void tickServer(
      Level level, BlockPos pos, BlockState state, CondenserBlockEntity condenser) {
    if (level.isClientSide()) {
      return;
    }

    condenser.checkLockAndUpdate(false);
    condenser.displayEmc = condenser.getStoredEmc();
    if (condenser.getLockInfo() != null) {
      condenser.condense();
    }

    condenser.updateComparators(level, pos);
  }

  private void checkLockAndUpdate(boolean force) {
    if (!force && this.loadIndex == EMCMappingHandler.getLoadIndex()) {
      return;
    }

    this.loadIndex = EMCMappingHandler.getLoadIndex();
    long lockEmc = this.lockInfo == null ? 0L : IEMCProxy.INSTANCE.getValue(this.lockInfo);
    this.requiredEmc = Math.max(0L, lockEmc);
    this.isAcceptingEmc = this.requiredEmc > 0L;
    // Clearing/changing the ghost target never clears the stored EMC, as in Forge.
    // Keep a valueless lock internally so an EMC remap can make it usable again.
    this.displayEmc = this.isAcceptingEmc ? this.getStoredEmc() : 0L;
  }

  protected void condense() {
    int i = 0;

    for (int slots = this.inputInventory.getSlots(); i < slots; i++) {
      ItemStack stack = this.inputInventory.getStackInSlot(i);
      if (!stack.isEmpty() && !this.isStackEqualToLock(stack)) {
        this.consumeInput(i, 1);
        break;
      }
    }

    this.produceOutput();
  }

  protected final void consumeInput(int slot, int maximumCount) {
    if (this.level == null || this.level.isClientSide() || this.getLockInfo() == null) {
      return;
    }

    ItemStack stack = this.inputInventory.getStackInSlot(slot);
    long sellEmc = IEMCProxy.INSTANCE.getSellValue(stack);
    int count = Math.min(maximumCount, stack.getCount());
    if (sellEmc > 0L) {
      // Do not destroy EMC that cannot fit, or overflow when MK2 sells a whole stack.
      count = (int) Math.min(count, this.getNeededEmc() / sellEmc);
    }
    if (count > 0) {
      ItemStack consumed = this.inputInventory.extractItem(slot, count, false);
      this.forceInsertEmc(sellEmc * consumed.getCount(), IEmcStorage.EmcAction.EXECUTE);
    }
  }

  protected final boolean produceOutput() {
    if (this.level == null || this.level.isClientSide()) {
      return false;
    }

    this.checkLockAndUpdate(false);
    ItemInfo lockInfo = this.getLockInfo();
    long cost = this.requiredEmc;
    if (lockInfo == null || cost <= 0L || this.getStoredEmc() < cost) {
      return false;
    }

    ItemStack output = lockInfo.createStack();
    if (output.isEmpty()
        || output.getCount() != 1
        || IEMCProxy.INSTANCE.getValue(output) != cost) {
      this.checkLockAndUpdate(true);
      return false;
    }
    if (!ItemHandlerHelper.insertItemStacked(this.outputInventory, output, true).isEmpty()) {
      return false;
    }

    // Tick processing and menu clicks run on the server thread. Reserve exactly one
    // item's cost and refund it if insertion fails; MK1 and MK2 use the same operation.
    long extracted = this.forceExtractEmc(cost, IEmcStorage.EmcAction.EXECUTE);
    if (extracted != cost) {
      this.forceInsertEmc(extracted, IEmcStorage.EmcAction.EXECUTE);
      return false;
    }
    if (!ItemHandlerHelper.insertItemStacked(this.outputInventory, output, false).isEmpty()) {
      this.forceInsertEmc(cost, IEmcStorage.EmcAction.EXECUTE);
      return false;
    }
    return true;
  }

  protected boolean hasSpace() {
    ItemInfo lockInfo = this.getLockInfo();
    return lockInfo != null
        && ItemHandlerHelper.insertItemStacked(this.outputInventory, lockInfo.createStack(), true)
            .isEmpty();
  }

  public boolean isStackEqualToLock(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }

    ItemInfo lockInfo = this.getLockInfo();
    return lockInfo == null
        ? false
        : lockInfo.equals(IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)));
  }

  public void setLockInfoFromPacket(@Nullable ItemInfo lockInfo) {
    // This is display-only client synchronization, never a server target setter.
    if (this.level != null && this.level.isClientSide()) {
      this.lockInfo = lockInfo;
    }
  }

  public boolean attemptCondenserSet(Player player) {
    return this.level != null
        && player.containerMenu instanceof CondenserContainer container
        && container.blockEntityMatches(this)
        && container.stillValid(player)
        && !player.isSpectator()
        && this.attemptCondenserSet(this.level, this.worldPosition, player);
  }

  private boolean attemptCondenserSet(@NotNull Level level, @NotNull BlockPos pos, Player player) {
    if (level.isClientSide()) {
      return false;
    }

    this.checkLockAndUpdate(false);
    if (this.getLockInfo() == null) {
      ItemStack stack = player.containerMenu.getCarried();
      if (!stack.isEmpty()) {
        ItemInfo sourceInfo = ItemInfo.fromStack(stack);
        ItemInfo reducedInfo = IEMCProxy.INSTANCE.getPersistentInfo(sourceInfo);
        if (!NeoForge.EVENT_BUS
            .post(new PlayerAttemptCondenserSetEvent(player, sourceInfo, reducedInfo))
            .isCanceled()) {
          this.lockInfo = reducedInfo;
          this.checkLockAndUpdate(true);
          this.markDirty(level, pos, false);
          return true;
        }

        return false;
      }

      if (this.lockInfo == null) {
        return false;
      }
    }

    this.lockInfo = null;
    this.checkLockAndUpdate(true);
    this.markDirty(level, pos, false);
    return true;
  }

  @Override
  public void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    this.inputInventory.deserializeNBT(
        tag.lookup(), tag.read("input", CompoundTag.CODEC).orElseGet(CompoundTag::new));
    this.lockInfo = (ItemInfo) tag.read("lock", ItemInfo.CODEC).orElse(null);
    // Loading may reuse an existing block entity. No cost/acceptance cache from the
    // previous target may survive, including when the EMC mapping index is unchanged.
    this.loadIndex = EMCMappingHandler.getLoadIndex() - 1;
    this.requiredEmc = 0L;
    this.displayEmc = 0L;
    this.isAcceptingEmc = false;
    if (this.level != null && !this.level.isClientSide()) {
      this.checkLockAndUpdate(true);
    }
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    tag.store(
        "input", CompoundTag.CODEC, this.inputInventory.serializeNBT(this.level.registryAccess()));
    if (this.lockInfo != null) {
      tag.store("lock", ItemInfo.CODEC, this.lockInfo);
    }
  }

  public AbstractContainerMenu createMenu(
      int windowId, @NotNull Inventory playerInventory, @NotNull Player playerIn) {
    return new CondenserContainer(windowId, playerInventory, this);
  }

  @NotNull
  public Component getDisplayName() {
    return TextComponentUtil.build(PEBlocks.CONDENSER);
  }
}
