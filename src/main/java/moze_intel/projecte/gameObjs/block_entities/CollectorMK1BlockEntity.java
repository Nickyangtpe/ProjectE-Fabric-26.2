package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.api.block_entity.IRelay;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.compat.items.wrapper.RangedWrapper;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.EnumCollectorTier;
import moze_intel.projecte.gameObjs.container.CollectorMK1Container;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
import org.jetbrains.annotations.Range;

public class CollectorMK1BlockEntity extends EmcBlockEntity implements MenuProvider {
  public static final ICapabilityProvider<
          CollectorMK1BlockEntity, @Nullable Direction, IItemHandler>
      INVENTORY_PROVIDER =
          (collector, side) -> {
            if (side == null) {
              return collector.joined;
            } else {
              return side.getAxis().isVertical()
                  ? collector.automationAuxSlots
                  : collector.automationInput;
            }
          };
  private final ItemStackHandler input =
      new EmcBlockEntity.StackHandler(this.getInvSize()) {
        @Override
        protected void onContentsChanged(int slot) {
          super.onContentsChanged(slot);
          CollectorMK1BlockEntity.this.needsCompacting = true;
        }
      };
  private final EmcBlockEntity.StackHandler auxSlots =
      new EmcBlockEntity.StackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
          super.onContentsChanged(slot);
          if (slot == 0) {
            CollectorMK1BlockEntity.this.needsCompacting = true;
          }
        }
      };
  private final CombinedInvWrapper toSort =
      new CombinedInvWrapper(new RangedWrapper(this.auxSlots, 0, 1), this.input);
  public static final int UPGRADING_SLOT = 0;
  public static final int UPGRADE_SLOT = 1;
  public static final int LOCK_SLOT = 2;
  private final IItemHandlerModifiable automationAuxSlots;
  private final IItemHandlerModifiable automationInput;
  private final IItemHandler joined;
  private final long emcGen;
  private double unprocessedEMC;
  private boolean hasChargeableItem;
  private boolean hasFuel;
  private boolean needsCompacting = true;

  public CollectorMK1BlockEntity(BlockPos pos, BlockState state) {
    this(PEBlockEntityTypes.COLLECTOR, pos, state, EnumCollectorTier.MK1);
  }

  public CollectorMK1BlockEntity(
      BlockEntityTypeRegistryObject<? extends CollectorMK1BlockEntity> type,
      BlockPos pos,
      BlockState state,
      EnumCollectorTier tier) {
    super(type, pos, state, tier.getStorage());
    this.emcGen = tier.getGenRate();
    this.automationInput =
        new WrappedItemHandler(this.input, WrappedItemHandler.WriteMode.IN) {
          @NotNull
          @Override
          public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return SlotPredicates.COLLECTOR_INV.test(stack)
                ? super.insertItem(slot, stack, simulate)
                : stack;
          }
        };
    this.automationAuxSlots =
        new WrappedItemHandler(this.auxSlots, WrappedItemHandler.WriteMode.OUT) {
          @NotNull
          @Override
          public ItemStack extractItem(int slot, int count, boolean simulate) {
            return slot == 1 ? super.extractItem(slot, count, simulate) : ItemStack.EMPTY;
          }
        };
    this.joined = new CombinedInvWrapper(this.automationInput, this.automationAuxSlots);
  }

  @Override
  protected boolean canAcceptEmc() {
    return this.hasFuel || this.hasChargeableItem;
  }

  public IItemHandler getInput() {
    return this.input;
  }

  public IItemHandler getAux() {
    return this.auxSlots;
  }

  protected int getInvSize() {
    return 8;
  }

  private ItemStack getUpgraded() {
    return this.auxSlots.getStackInSlot(1);
  }

  private ItemStack getLock() {
    return this.auxSlots.getStackInSlot(2);
  }

  private ItemStack getUpgrading() {
    return this.auxSlots.getStackInSlot(0);
  }

  public void clearLocked() {
    this.auxSlots.setStackInSlot(2, ItemStack.EMPTY);
  }

  @Override
  protected boolean emcAffectsComparators() {
    return true;
  }

  public static void tickServer(
      Level level, BlockPos pos, BlockState state, CollectorMK1BlockEntity collector) {
    if (collector.needsCompacting) {
      ItemHelper.compactInventory(collector.toSort);
      collector.needsCompacting = false;
    }

    collector.checkFuelOrKlein();
    collector.updateEmc(level, pos);
    collector.rotateUpgraded();
    collector.updateComparators(level, pos);
  }

  private void rotateUpgraded() {
    ItemStack upgraded = this.getUpgraded();
    if (!upgraded.isEmpty()) {
      ItemStack lock = this.getLock();
      if (lock.isEmpty()
          || upgraded.getItem() != lock.getItem()
          || upgraded.getCount() >= upgraded.getMaxStackSize()) {
        this.auxSlots.setStackInSlot(
            1, ItemHandlerHelper.insertItemStacked(this.input, upgraded.copy(), false));
      }
    }
  }

  private void checkFuelOrKlein() {
    ItemStack upgrading = this.getUpgrading();
    if (!upgrading.isEmpty()) {
      IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(upgrading, null);
      if (emcHolder != null) {
        if (emcHolder.getNeededEmc(upgrading) > 0L) {
          this.hasChargeableItem = true;
          this.hasFuel = false;
        } else {
          this.hasChargeableItem = false;
        }
      } else {
        this.hasFuel = FuelMapper.isStackFuel(upgrading);
        this.hasChargeableItem = false;
      }
    } else {
      this.hasFuel = false;
      this.hasChargeableItem = false;
    }
  }

  private void updateEmc(@NotNull Level level, @NotNull BlockPos pos) {
    if (!this.hasMaxedEmc()) {
      this.unprocessedEMC =
          this.unprocessedEMC + (float) this.emcGen * (getSunLevel(level, pos) / 320.0F);
      if (this.unprocessedEMC >= 1.0) {
        this.unprocessedEMC =
            this.unprocessedEMC
                - this.forceInsertEmc((long) this.unprocessedEMC, IEmcStorage.EmcAction.EXECUTE);
      }

      this.markDirty(level, pos, false);
    }

    if (this.getStoredEmc() > 0L) {
      ItemStack upgrading = this.getUpgrading();
      if (this.hasChargeableItem) {
        IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(upgrading, null);
        if (emcHolder != null) {
          long actualInserted =
              emcHolder.insertEmc(
                  upgrading,
                  Math.min(this.getStoredEmc(), this.emcGen),
                  IEmcStorage.EmcAction.EXECUTE);
          this.forceExtractEmc(actualInserted, IEmcStorage.EmcAction.EXECUTE);
        }

        return;
      }

      if (this.hasFuel) {
        ItemStack fuelUpgrade = FuelMapper.getFuelUpgrade(upgrading);
        if (!fuelUpgrade.isEmpty()) {
          ItemStack lock = this.getLock();
          ItemStack result = lock.isEmpty() ? fuelUpgrade : lock.copy();
          long upgradeCost =
              IEMCProxy.INSTANCE.getValue(result) - IEMCProxy.INSTANCE.getValue(upgrading);
          if (upgradeCost >= 0L && this.getStoredEmc() >= upgradeCost) {
            ItemStack upgrade = this.getUpgraded();
            if (upgrade.isEmpty()) {
              this.forceExtractEmc(upgradeCost, IEmcStorage.EmcAction.EXECUTE);
              this.auxSlots.setStackInSlot(1, result);
              upgrading.shrink(1);
            } else if (result.is(upgrade.getItem())
                && upgrade.getCount() < upgrade.getMaxStackSize()) {
              this.forceExtractEmc(upgradeCost, IEmcStorage.EmcAction.EXECUTE);
              upgrade.grow(1);
              upgrading.shrink(1);
              this.auxSlots.onContentsChanged(1);
            }
          }

          return;
        }
      }

      long toSend = this.getStoredEmc() < this.emcGen ? this.getStoredEmc() : this.emcGen;
      this.sendToAllAcceptors(level, pos, toSend);
      sendRelayBonus(level, pos);
    }
  }

  public @Range(from = 0L, to = Long.MAX_VALUE) long getEmcToNextGoal() {
    ItemStack lock = this.getLock();
    ItemStack upgrading = this.getUpgrading();
    long targetEmc;
    if (lock.isEmpty()) {
      targetEmc = IEMCProxy.INSTANCE.getValue(FuelMapper.getFuelUpgrade(upgrading));
    } else {
      targetEmc = IEMCProxy.INSTANCE.getValue(lock);
    }

    return Math.max(targetEmc - IEMCProxy.INSTANCE.getValue(upgrading), 0L);
  }

  public long getItemCharge() {
    ItemStack upgrading = this.getUpgrading();
    IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(upgrading, null);
    return emcHolder != null ? emcHolder.getStoredEmc(upgrading) : -1L;
  }

  public double getItemChargeProportion() {
    long charge = this.getItemCharge();
    if (charge <= 0L) {
      return -1.0;
    }

    ItemStack upgrading = this.getUpgrading();
    IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(upgrading, null);
    if (emcHolder == null) {
      return -1.0;
    }

    long max = emcHolder.getMaximumEmc(upgrading);
    return charge >= max ? 1.0 : (double) charge / max;
  }

  public int getSunLevel() {
    return this.level == null ? 0 : getSunLevel(this.level, this.worldPosition);
  }

  public static int getSunLevel(@NotNull Level level, @NotNull BlockPos pos) {
    return level.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
        ? 16
        : level.getMaxLocalRawBrightness(pos.above()) + 1;
  }

  public double getFuelProgress() {
    ItemStack upgrading = this.getUpgrading();
    if (!FuelMapper.isStackFuel(upgrading)) {
      return 0.0;
    }

    ItemStack lock = this.getLock();
    long reqEmc;
    if (!lock.isEmpty()) {
      reqEmc = IEMCProxy.INSTANCE.getValue(lock) - IEMCProxy.INSTANCE.getValue(upgrading);
      if (reqEmc < 0L) {
        return 0.0;
      }
    } else {
      ItemStack fuelUpgrade = FuelMapper.getFuelUpgrade(upgrading);
      if (fuelUpgrade.isEmpty()) {
        return 0.0;
      }

      reqEmc = IEMCProxy.INSTANCE.getValue(fuelUpgrade) - IEMCProxy.INSTANCE.getValue(upgrading);
    }

    return this.getStoredEmc() >= reqEmc ? 1.0 : (double) this.getStoredEmc() / reqEmc;
  }

  @Override
  public void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    this.unprocessedEMC = tag.getDoubleOr("unprocessed_emc", 0.0);
    tag.read("input", CompoundTag.CODEC)
        .ifPresent(data -> this.input.deserializeNBT(tag.lookup(), data));
    tag.read("aux_slots", CompoundTag.CODEC)
        .ifPresent(data -> this.auxSlots.deserializeNBT(tag.lookup(), data));
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    tag.putDouble("unprocessed_emc", this.unprocessedEMC);
    tag.store("input", CompoundTag.CODEC, this.input.serializeNBT(this.level.registryAccess()));
    tag.store(
        "aux_slots", CompoundTag.CODEC, this.auxSlots.serializeNBT(this.level.registryAccess()));
  }

  private static void sendRelayBonus(@NotNull Level level, @NotNull BlockPos pos) {
    for (Direction dir : Constants.DIRECTIONS) {
      BlockPos relayPos = pos.relative(dir);
      if (WorldHelper.getBlockEntity(level, relayPos) instanceof IRelay relay) {
        relay.addBonus(level, relayPos);
      }
    }
  }

  @NotNull
  public AbstractContainerMenu createMenu(
      int windowId, @NotNull Inventory playerInventory, @NotNull Player playerIn) {
    return new CollectorMK1Container(windowId, playerInventory, this);
  }

  @NotNull
  public Component getDisplayName() {
    return TextComponentUtil.build(PEBlocks.COLLECTOR);
  }
}
