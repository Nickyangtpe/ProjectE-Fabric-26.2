package moze_intel.projecte.gameObjs.container.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.SearchQueryParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TransmutationInventory extends CombinedInvWrapper {
  public final Player player;
  public final IKnowledgeProvider provider;
  private final IItemHandlerModifiable inputLocks;
  private final IItemHandlerModifiable learning;
  public final IItemHandlerModifiable outputs;
  private static final int MAX_MATTER_DISPLAY = 12;
  private static final int MAX_FUEL_DISPLAY = 4;
  private static final int LOCK_INDEX = 8;
  private static final int FUEL_START = 12;
  public int learnFlag = 0;
  public int unlearnFlag = 0;
  public SearchQueryParser.ISearchQuery filter = SearchQueryParser.ISearchQuery.INVALID;
  private int searchPage = 0;
  private boolean hasNextPage;
  private long lastAvailableEmc;

  public TransmutationInventory(Player player) {
    super(
        (IItemHandlerModifiable)
            Objects.requireNonNull(PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null))
                .getInputAndLocks(),
        new ItemStackHandler(2),
        new ItemStackHandler(16));
    this.player = player;
    this.provider = Objects.requireNonNull(PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null));
    this.inputLocks = this.itemHandler[0];
    this.learning = this.itemHandler[1];
    this.outputs = this.itemHandler[2];
    if (this.isClient()) {
      this.updateClientTargets(false);
    }
  }

  public boolean isServer() {
    return !this.isClient();
  }

  public boolean isClient() {
    return this.player.level().isClientSide();
  }

  public void handleKnowledge(ItemStack stack) {
    if (!stack.isEmpty()) {
      this.handleKnowledge(ItemInfo.fromStack(stack));
    }
  }

  public void handleKnowledge(ItemInfo info) {
    ItemInfo cleanedInfo = IEMCProxy.INSTANCE.getPersistentInfo(info);
    if (!this.provider.hasKnowledge(cleanedInfo)
        && !NeoForge.EVENT_BUS
            .post(new PlayerAttemptLearnEvent(this.player, info, cleanedInfo))
            .isCanceled()
        && this.provider.addKnowledge(cleanedInfo)) {
      this.provider.syncKnowledgeChange((ServerPlayer) this.player, cleanedInfo, true);
    }
  }

  public void itemLearned(ItemInfo learnedItem) {
    this.learnFlag = 300;
    this.unlearnFlag = 0;
    if (!this.isServer()) {
      long learnedItemEmc = IEMCProxy.INSTANCE.getValue(learnedItem);
      if (learnedItemEmc == 0L || learnedItem.getItem().is(PEItems.TOME_OF_KNOWLEDGE.getKey())) {
        this.resetSearchPage();
        this.updateClientTargets(false);
      } else if (this.doesItemMatchFilter(learnedItem)) {
        boolean learnedFuel = learnedItem.getItem().is(PETags.Items.COLLECTOR_FUEL);
        if (learnedFuel) {
          long lastFuelEmc =
              IEMCProxy.INSTANCE.getValue(this.outputs.getStackInSlot(this.outputs.getSlots() - 1));
          if (learnedItemEmc < lastFuelEmc) {
            this.hasNextPage = true;
            return;
          }
        } else if (learnedItemEmc < IEMCProxy.INSTANCE.getValue(this.outputs.getStackInSlot(11))) {
          this.hasNextPage = true;
          return;
        }

        long availableEmc = this.getAvailableEmcAsLong();
        if (learnedItemEmc <= availableEmc) {
          ItemStack lockStack = this.inputLocks.getStackInSlot(8);
          if (!lockStack.isEmpty()) {
            ItemInfo lockInfo = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(lockStack));
            long lockEmc = IEMCProxy.INSTANCE.getValue(lockInfo);
            if (lockEmc != 0L && lockEmc < availableEmc && learnedItemEmc > lockEmc) {
              return;
            }
          }

          this.updateClientTargets(availableEmc);
        }
      }
    }
  }

  public void handleUnlearn(ItemStack stack) {
    if (!stack.isEmpty()) {
      this.handleUnlearn(ItemInfo.fromStack(stack));
    }
  }

  public void handleUnlearn(ItemInfo info) {
    ItemInfo cleanedInfo = IEMCProxy.INSTANCE.getPersistentInfo(info);
    if (this.provider.hasKnowledge(cleanedInfo) && this.provider.removeKnowledge(cleanedInfo)) {
      this.provider.syncKnowledgeChange((ServerPlayer) this.player, cleanedInfo, false);
    }
  }

  public void itemUnlearned(ItemInfo unlearnedItem) {
    this.unlearnFlag = 300;
    this.learnFlag = 0;
    if (!this.isServer()) {
      long unlearnedItemEmc = IEMCProxy.INSTANCE.getValue(unlearnedItem);
      if (unlearnedItemEmc == 0L
          || unlearnedItem.getItem().is(PEItems.TOME_OF_KNOWLEDGE.getKey())) {
        this.resetSearchPage();
        this.updateClientTargets(false);
      } else if (this.doesItemMatchFilter(unlearnedItem)) {
        if (unlearnedItem.getItem().is(PETags.Items.COLLECTOR_FUEL)) {
          this.maybeUpdateClientTargets(
              unlearnedItem, unlearnedItemEmc, 12, this.outputs.getSlots());
        } else {
          this.maybeUpdateClientTargets(unlearnedItem, unlearnedItemEmc, 0, 12);
        }
      }
    }
  }

  private void maybeUpdateClientTargets(
      ItemInfo unlearnedItem, long unlearnedItemEmc, int slot, int slots) {
    long availableEmc = this.getAvailableEmcAsLong();
    if (unlearnedItemEmc <= availableEmc) {
      int firstNonLockSlot = slot;
      ItemStack lockStack = this.inputLocks.getStackInSlot(8);
      long lockEmc = 0L;
      if (!lockStack.isEmpty()) {
        ItemInfo lockInfo = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(lockStack));
        lockEmc = IEMCProxy.INSTANCE.getValue(lockInfo);
        if (lockEmc > availableEmc) {
          lockStack = ItemStack.EMPTY;
        } else {
          firstNonLockSlot++;
        }
      }

      while (slot < slots) {
        ItemStack stack = this.outputs.getStackInSlot(slot);
        if (stack.isEmpty()) {
          break;
        }

        if (stack.is(unlearnedItem.getItem())
            && stack.getComponentsPatch().equals(unlearnedItem.getComponentsPatch())) {
          if (this.hasPreviousPage()) {
            if (!lockStack.isEmpty() && slot == firstNonLockSlot - 1) {
              this.resetSearchPage();
            } else if (slot == firstNonLockSlot && slot + 1 < slots) {
              ItemStack next = this.outputs.getStackInSlot(slot + 1);
              if (next.isEmpty()) {
                this.searchPage--;
              }
            }
          }

          this.updateClientTargets(availableEmc);
          return;
        }

        slot++;
      }

      if (lockStack.isEmpty()) {
        long maxDisplayedEmc = this.getMaxDisplayedEmc();
        if (unlearnedItemEmc >= maxDisplayedEmc) {
          if (this.hasPreviousPage() && slot - 1 == firstNonLockSlot) {
            this.searchPage--;
          }

          this.updateClientTargets(availableEmc);
        }
      } else if (lockEmc > 0L && unlearnedItemEmc == lockEmc) {
        if (this.hasPreviousPage() && slot - 1 == firstNonLockSlot) {
          this.searchPage--;
        }

        this.updateClientTargets(availableEmc);
      }
    }
  }

  public void checkForUpdates() {
    long availableEmc = this.getAvailableEmcAsLong();
    if (this.getMaxDisplayedEmc() > availableEmc) {
      this.updateClientTargets(availableEmc);
    }
  }

  private long getMaxDisplayedEmc() {
    long matterEmc = IEMCProxy.INSTANCE.getValue(this.outputs.getStackInSlot(0));
    long fuelEmc = IEMCProxy.INSTANCE.getValue(this.outputs.getStackInSlot(12));
    return Math.max(matterEmc, fuelEmc);
  }

  public void updateClientTargets(boolean checkForEmcChange) {
    if (this.isClient()) {
      long availableEmc = this.getAvailableEmcAsLong();
      if (!checkForEmcChange) {
        this.updateClientTargets(availableEmc);
      } else if (this.lastAvailableEmc != availableEmc) {
        this.updateClientTargets(availableEmc);
      }
    }
  }

  private void updateClientTargets(long availableEMC) {
    this.lastAvailableEmc = availableEMC;
    int i = 0;

    for (int slots = this.outputs.getSlots(); i < slots; i++) {
      this.outputs.setStackInSlot(i, ItemStack.EMPTY);
    }

    ItemStack lockStack = this.inputLocks.getStackInSlot(8);
    int matterCounter = 0;
    int fuelCounter = 0;
    record EmcData(ItemInfo info, long emc) {}

    Predicate<EmcData> filterPredicate;

    if (lockStack.isEmpty()) {
      filterPredicate = data -> data.emc() > 0L && data.emc() <= availableEMC;
    } else {
      ItemInfo lockInfo = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(lockStack));
      long reqEmc = IEMCProxy.INSTANCE.getValue(lockInfo);
      if (availableEMC < reqEmc || reqEmc == 0L) {
        filterPredicate = data -> data.emc() > 0L && data.emc() <= availableEMC;
      } else if (this.provider.hasKnowledge(lockInfo)) {
        if (lockInfo.getItem().is(PETags.Items.COLLECTOR_FUEL)) {
          this.outputs.setStackInSlot(12 + fuelCounter++, lockInfo.createStack());
        } else {
          this.outputs.setStackInSlot(matterCounter++, lockInfo.createStack());
        }

        filterPredicate =
            data -> {
              long emc = data.emc();
              if (emc > 0L) {
                if (emc < reqEmc) {
                  return true;
                }

                if (emc == reqEmc) {
                  return !data.info().equals(lockInfo);
                }
              }

              return false;
            };
      } else {
        filterPredicate = data -> data.emc() > 0L && data.emc() <= reqEmc;
      }
    }

    List<ItemInfo> knowledge =
        this.provider.getKnowledge().stream()
            .map(infox -> new EmcData(infox, IEMCProxy.INSTANCE.getValue(infox)))
            .filter(filterPredicate)
            .sorted(Comparator.comparingLong(EmcData::emc).reversed())
            .map(EmcData::info)
            .toList();
    int fuelPageCounter = 0;
    int matterPageCounter = 0;
    int desiredFuelPage = this.searchPage * (4 - fuelCounter);
    int desiredMatterPage = this.searchPage * (12 - matterCounter);
    this.hasNextPage = false;

    for (ItemInfo info : knowledge) {
      if (info.getItem().is(PETags.Items.COLLECTOR_FUEL)) {
        if (fuelCounter < 4) {
          if (this.doesItemMatchFilter(info)) {
            if (fuelPageCounter == desiredFuelPage) {
              this.outputs.setStackInSlot(12 + fuelCounter++, info.createStack());
            } else {
              fuelPageCounter++;
            }
          }
        } else {
          this.hasNextPage = true;
          if (matterCounter == 12) {
            break;
          }
        }
      } else if (matterCounter < 12) {
        if (this.doesItemMatchFilter(info)) {
          if (matterPageCounter == desiredMatterPage) {
            this.outputs.setStackInSlot(matterCounter++, info.createStack());
          } else {
            matterPageCounter++;
          }
        }
      } else {
        this.hasNextPage = true;
        if (fuelCounter == 4) {
          break;
        }
      }
    }
  }

  private boolean doesItemMatchFilter(ItemInfo info) {
    return this.filter.isInvalid()
        || this.filter.test(this.player.level(), this.player, info.createStack());
  }

  public void writeIntoOutputSlot(int slot, ItemStack item) {
    if (slot >= 0 && slot < this.outputs.getSlots()) {
      long emcValue = IEMCProxy.INSTANCE.getValue(item);
      if (emcValue > 0L
          && emcValue <= this.getAvailableEmcAsLong()
          && this.provider.hasKnowledge(item)) {
        this.outputs.setStackInSlot(slot, item);
      } else {
        this.outputs.setStackInSlot(slot, ItemStack.EMPTY);
      }
    }
  }

  public int getOutputSlotCount() {
    return this.outputs.getSlots();
  }

  public void addEmc(BigInteger value) {
    if (value.signum() != 0) {
      if (value.signum() == -1) {
        this.removeEmc(value.negate());
      } else {
        IntList inputLocksChanged = new IntArrayList();
        int slotIndex = 0;

        for (int slots = this.inputLocks.getSlots(); slotIndex < slots; slotIndex++) {
          if (slotIndex != 8) {
            ItemStack stack = this.inputLocks.getStackInSlot(slotIndex);
            IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
            if (emcHolder != null) {
              long shrunkenValue = MathUtils.clampToLong(value);
              long actualInserted =
                  emcHolder.insertEmc(stack, shrunkenValue, IEmcStorage.EmcAction.EXECUTE);
              if (actualInserted > 0L) {
                inputLocksChanged.add(slotIndex);
                value = value.subtract(BigInteger.valueOf(actualInserted));
                if (value.signum() == 0) {
                  this.syncChangedSlots(inputLocksChanged, IKnowledgeProvider.TargetUpdateType.ALL);
                  return;
                }
              }
            }
          }
        }

        this.syncChangedSlots(inputLocksChanged, IKnowledgeProvider.TargetUpdateType.NONE);
        this.updateEmcAndSync(this.provider.getEmc().add(value));
      }
    }
  }

  public void removeEmc(BigInteger value) {
    if (value.signum() != 0) {
      if (value.signum() == -1) {
        this.addEmc(value.negate());
      } else {
        BigInteger currentEmc = this.provider.getEmc();
        if (value.compareTo(currentEmc) > 0) {
          IntList inputLocksChanged = new IntArrayList();
          BigInteger toRemove = value.subtract(currentEmc);
          value = currentEmc;
          int slotIndex = 0;

          for (int slots = this.inputLocks.getSlots(); slotIndex < slots; slotIndex++) {
            if (slotIndex != 8) {
              ItemStack stack = this.inputLocks.getStackInSlot(slotIndex);
              IItemEmcHolder emcHolder =
                  PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
              if (emcHolder != null) {
                long shrunkenToRemove = MathUtils.clampToLong(toRemove);
                long actualExtracted =
                    emcHolder.extractEmc(stack, shrunkenToRemove, IEmcStorage.EmcAction.EXECUTE);
                if (actualExtracted > 0L) {
                  inputLocksChanged.add(slotIndex);
                  toRemove = toRemove.subtract(BigInteger.valueOf(actualExtracted));
                  if (toRemove.signum() == 0) {
                    this.syncChangedSlots(
                        inputLocksChanged, IKnowledgeProvider.TargetUpdateType.IF_NEEDED);
                    if (currentEmc.signum() == 1) {
                      this.updateEmcAndSync(BigInteger.ZERO);
                    }

                    return;
                  }
                }
              }
            }
          }

          this.syncChangedSlots(inputLocksChanged, IKnowledgeProvider.TargetUpdateType.NONE);
        }

        this.updateEmcAndSync(currentEmc.subtract(value));
      }
    }
  }

  public void syncChangedSlots(
      IntList slotsChanged, IKnowledgeProvider.TargetUpdateType updateTargets) {
    this.provider.syncInputAndLocks((ServerPlayer) this.player, slotsChanged, updateTargets);
  }

  private void updateEmcAndSync(BigInteger emc) {
    if (emc.signum() == -1) {
      emc = BigInteger.ZERO;
    }

    this.provider.setEmc(emc);
    this.provider.syncEmc((ServerPlayer) this.player);
    PlayerHelper.updateScore((ServerPlayer) this.player, PlayerHelper.SCOREBOARD_EMC, emc);
  }

  public IItemHandlerModifiable getHandlerForSlot(int slot) {
    return super.getHandlerFromIndex(super.getIndexForSlot(slot));
  }

  public int getIndexFromSlot(int slot) {
    for (IItemHandlerModifiable h : this.itemHandler) {
      if (slot >= h.getSlots()) {
        slot -= h.getSlots();
      }
    }

    return slot;
  }

  public long getAvailableEmcAsLong() {
    long emc = MathUtils.clampToLong(this.provider.getEmc());
    if (emc != Long.MAX_VALUE && this.inputLocks.getSlots() != 0) {
      long emcToMax = Long.MAX_VALUE - emc;
      int i = 0;

      for (int slots = this.inputLocks.getSlots(); i < slots; i++) {
        if (i != 8) {
          ItemStack stack = this.inputLocks.getStackInSlot(i);
          IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
          if (emcHolder != null) {
            long storedEmc = emcHolder.getStoredEmc(stack);
            if (storedEmc >= emcToMax) {
              return Long.MAX_VALUE;
            }

            emcToMax -= storedEmc;
          }
        }
      }

      return Long.MAX_VALUE - emcToMax;
    } else {
      return emc;
    }
  }

  public BigInteger getAvailableEmc() {
    BigInteger emc = this.provider.getEmc();
    int i = 0;

    for (int slots = this.inputLocks.getSlots(); i < slots; i++) {
      if (i != 8) {
        ItemStack stack = this.inputLocks.getStackInSlot(i);
        IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
        if (emcHolder != null) {
          emc = emc.add(BigInteger.valueOf(emcHolder.getStoredEmc(stack)));
        }
      }
    }

    return emc;
  }

  public void updateFilter(String text) {
    String search = text.trim().toLowerCase(Locale.ROOT);
    SearchQueryParser.ISearchQuery query = SearchQueryParser.parse(search);
    if (!this.filter.equals(query)) {
      this.filter = query;
      this.resetSearchPage();
      this.updateClientTargets(false);
    }
  }

  public boolean hasPreviousPage() {
    return this.searchPage > 0;
  }

  public boolean hasNextPage() {
    return this.hasNextPage;
  }

  private void resetSearchPage() {
    this.searchPage = 0;
  }

  public void previousPage() {
    if (this.hasPreviousPage()) {
      this.searchPage--;
      this.updateClientTargets(false);
    }
  }

  public void nextPage() {
    if (this.hasNextPage()) {
      this.searchPage++;
      this.updateClientTargets(false);
    }
  }
}
