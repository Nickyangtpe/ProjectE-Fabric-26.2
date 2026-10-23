package moze_intel.projecte.gameObjs.block_entities;

import java.util.List;
import moze_intel.projecte.api.block_entity.IRelay;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.gameObjs.EnumRelayTier;
import moze_intel.projecte.gameObjs.container.RelayMK1Container;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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

public class RelayMK1BlockEntity extends EmcBlockEntity implements MenuProvider, IRelay {
  public static final ICapabilityProvider<RelayMK1BlockEntity, @Nullable Direction, IItemHandler>
      INVENTORY_PROVIDER =
          (relay, side) -> {
            if (side == null) {
              return relay.joined;
            } else {
              return side.getAxis().isVertical() ? relay.automationOutput : relay.automationInput;
            }
          };
  private final EmcBlockEntity.CompactableStackHandler input;
  private final ItemStackHandler output = new EmcBlockEntity.StackHandler(1);
  private final IItemHandlerModifiable automationOutput;
  private final IItemHandlerModifiable automationInput;
  private final IItemHandler joined;
  private final long chargeRate;
  private double bonusEMC;

  public RelayMK1BlockEntity(BlockPos pos, BlockState state) {
    this(PEBlockEntityTypes.RELAY, pos, state, 7, EnumRelayTier.MK1);
  }

  RelayMK1BlockEntity(
      BlockEntityTypeRegistryObject<? extends RelayMK1BlockEntity> type,
      BlockPos pos,
      BlockState state,
      int sizeInv,
      EnumRelayTier tier) {
    super(type, pos, state, tier.getStorage());
    this.chargeRate = tier.getChargeRate();
    this.input =
        new EmcBlockEntity.CompactableStackHandler(sizeInv) {
          @NotNull
          @Override
          public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return SlotPredicates.RELAY_INV.test(stack)
                ? super.insertItem(slot, stack, simulate)
                : stack;
          }
        };
    this.automationInput = new WrappedItemHandler(this.input, WrappedItemHandler.WriteMode.IN);
    this.automationOutput =
        new WrappedItemHandler(this.output, WrappedItemHandler.WriteMode.IN_OUT) {
          @NotNull
          @Override
          public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return SlotPredicates.EMC_HOLDER.test(stack)
                ? super.insertItem(slot, stack, simulate)
                : stack;
          }

          @NotNull
          @Override
          public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = this.getStackInSlot(slot);
            IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
            return emcHolder != null && emcHolder.getNeededEmc(stack) > 0L
                ? ItemStack.EMPTY
                : super.extractItem(slot, amount, simulate);
          }
        };
    this.joined = new CombinedInvWrapper(this.automationInput, this.automationOutput);
  }

  @Override
  public boolean isRelay() {
    return true;
  }

  private ItemStack getCharging() {
    return this.output.getStackInSlot(0);
  }

  private ItemStack getBurn() {
    return this.input.getStackInSlot(0);
  }

  public IItemHandler getInput() {
    return this.input;
  }

  public IItemHandler getOutput() {
    return this.output;
  }

  @Override
  protected List<IItemHandlerModifiable> getInventoriesForDrops() {
    return List.of(this.input, this.output);
  }

  @Override
  protected boolean emcAffectsComparators() {
    return true;
  }

  public static void tickServer(
      Level level, BlockPos pos, BlockState state, RelayMK1BlockEntity relay) {
    relay.sendToAllAcceptors(level, pos, relay.getAvailableCharge());
    relay.input.compact();
    ItemStack stack = relay.getBurn();
    if (!stack.isEmpty()) {
      IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
      if (emcHolder != null) {
        long simulatedVal =
            relay.forceInsertEmc(
                emcHolder.extractEmc(stack, relay.chargeRate, IEmcStorage.EmcAction.SIMULATE),
                IEmcStorage.EmcAction.SIMULATE);
        if (simulatedVal > 0L) {
          relay.forceInsertEmc(
              emcHolder.extractEmc(stack, simulatedVal, IEmcStorage.EmcAction.EXECUTE),
              IEmcStorage.EmcAction.EXECUTE);
        }
      } else {
        long emcVal = IEMCProxy.INSTANCE.getSellValue(stack);
        if (emcVal > 0L && emcVal <= relay.getNeededEmc()) {
          relay.forceInsertEmc(emcVal, IEmcStorage.EmcAction.EXECUTE);
          relay.getBurn().shrink(1);
          relay.input.onContentsChanged(0);
        }
      }
    }

    if (relay.getStoredEmc() > 0L) {
      ItemStack chargeable = relay.getCharging();
      IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(chargeable, null);
      if (emcHolder != null) {
        long actualSent =
            emcHolder.insertEmc(
                chargeable, relay.getAvailableCharge(), IEmcStorage.EmcAction.EXECUTE);
        relay.forceExtractEmc(actualSent, IEmcStorage.EmcAction.EXECUTE);
      }
    }

    relay.updateComparators(level, pos);
  }

  private long getAvailableCharge() {
    return Math.min(this.chargeRate, this.getStoredEmc());
  }

  public double getItemChargeProportion() {
    ItemStack charging = this.getCharging();
    IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(charging, null);
    return emcHolder != null
        ? (double) emcHolder.getStoredEmc(charging) / emcHolder.getMaximumEmc(charging)
        : 0.0;
  }

  public double getInputBurnProportion() {
    ItemStack burn = this.getBurn();
    if (burn.isEmpty()) {
      return 0.0;
    }

    IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(burn, null);
    return emcHolder != null
        ? (double) emcHolder.getStoredEmc(burn) / emcHolder.getMaximumEmc(burn)
        : (double) burn.getCount() / burn.getMaxStackSize();
  }

  @Override
  public void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    tag.read("input", CompoundTag.CODEC)
        .ifPresent(data -> this.input.deserializeNBT(tag.lookup(), data));
    tag.read("output", CompoundTag.CODEC)
        .ifPresent(data -> this.output.deserializeNBT(tag.lookup(), data));
    this.bonusEMC = tag.getDoubleOr("bonus_emc", 0.0);
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    tag.store("input", CompoundTag.CODEC, this.input.serializeNBT(this.level.registryAccess()));
    tag.store("output", CompoundTag.CODEC, this.output.serializeNBT(this.level.registryAccess()));
    tag.putDouble("bonus_emc", this.bonusEMC);
  }

  @Override
  public double getBonusToAdd() {
    return 0.05;
  }

  @Override
  public void addBonus(@NotNull Level level, @NotNull BlockPos pos) {
    this.bonusEMC = this.bonusEMC + this.getBonusToAdd();
    if (this.bonusEMC >= 1.0) {
      long emcToInsert = (long) this.bonusEMC;
      this.forceInsertEmc(emcToInsert, IEmcStorage.EmcAction.EXECUTE);
      this.bonusEMC -= emcToInsert;
    }

    this.markDirty(level, pos, false);
  }

  @NotNull
  public AbstractContainerMenu createMenu(
      int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
    return new RelayMK1Container(windowId, playerInventory, this);
  }

  @NotNull
  public Component getDisplayName() {
    return PELang.GUI_RELAY_MK1.translate();
  }
}
