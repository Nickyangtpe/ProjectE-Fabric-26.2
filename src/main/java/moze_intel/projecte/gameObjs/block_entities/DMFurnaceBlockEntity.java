package moze_intel.projecte.gameObjs.block_entities;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.compat.capabilities.BlockCapabilityCache;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.common.Tags;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.gameObjs.blocks.MatterFurnace;
import moze_intel.projecte.gameObjs.container.DMFurnaceContainer;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeManager.CachedCheck;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class DMFurnaceBlockEntity extends EmcBlockEntity
    implements MenuProvider, RecipeCraftingHolder {
  public static final ICapabilityProvider<DMFurnaceBlockEntity, @Nullable Direction, IItemHandler>
      INVENTORY_PROVIDER =
          (furnace, side) -> {
            if (side == null) {
              return furnace.joined;
            } else if (side == Direction.UP) {
              return furnace.automationInput;
            } else {
              return side == Direction.DOWN ? furnace.automationOutput : furnace.automationSides;
            }
          };
  private static final long EMC_CONSUMPTION = 2L;
  private final EmcBlockEntity.CompactableStackHandler inputInventory =
      new EmcBlockEntity.CompactableStackHandler(this.getInvSize()) {
        private ItemStack oldInput = ItemStack.EMPTY;

        @Override
        protected void onLoad() {
          this.oldInput = this.getStackInSlot(0).copy();
        }

        @Override
        protected void onContentsChanged(int slot) {
          super.onContentsChanged(slot);
          if (slot == 0) {
            ItemStack input = this.getStackInSlot(0);
            if (!ItemStack.isSameItemSameComponents(this.oldInput, input)) {
              DMFurnaceBlockEntity.RecipeResult recipeResult =
                  DMFurnaceBlockEntity.this.getSmeltingRecipe(input);
              DMFurnaceBlockEntity.this.cookingTotalTime =
                  DMFurnaceBlockEntity.this.getTotalCookTime(recipeResult);
              DMFurnaceBlockEntity.this.cookingProgress = 0;
              this.oldInput = input.copy();
            }
          }
        }
      };
  private final EmcBlockEntity.CompactableStackHandler outputInventory =
      new EmcBlockEntity.CompactableStackHandler(this.getInvSize());
  private final EmcBlockEntity.StackHandler fuelInv = new EmcBlockEntity.StackHandler(1);
  private final IItemHandler joined;
  private final IItemHandlerModifiable automationInput;
  private final IItemHandlerModifiable automationOutput;
  private final IItemHandler automationSides;
  protected final int ticksBeforeSmelt;
  private final int efficiencyBonus;
  private final Object2IntOpenHashMap<Identifier> recipesUsed = new Object2IntOpenHashMap<>();
  private final CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck;
  @Nullable private BlockCapabilityCache<IItemHandler, Direction> pullTarget;
  @Nullable private BlockCapabilityCache<IItemHandler, Direction> pushTarget;
  public int litTime;
  public int litDuration;
  public int cookingProgress;
  public int cookingTotalTime;

  public DMFurnaceBlockEntity(BlockPos pos, BlockState state) {
    this(PEBlockEntityTypes.DARK_MATTER_FURNACE, pos, state, 10, 3);
  }

  protected DMFurnaceBlockEntity(
      BlockEntityTypeRegistryObject<? extends DMFurnaceBlockEntity> type,
      BlockPos pos,
      BlockState state,
      int ticksBeforeSmelt,
      int efficiencyBonus) {
    super(type, pos, state, 64L);
    this.ticksBeforeSmelt = ticksBeforeSmelt;
    this.efficiencyBonus = efficiencyBonus;
    this.quickCheck = RecipeManager.createCheck(RecipeType.SMELTING);
    this.automationInput =
        new WrappedItemHandler(this.inputInventory, WrappedItemHandler.WriteMode.IN) {
          @NotNull
          @Override
          public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return DMFurnaceBlockEntity.this.hasSmeltingResult(stack)
                ? super.insertItem(slot, stack, simulate)
                : stack;
          }
        };
    this.automationOutput =
        new WrappedItemHandler(this.outputInventory, WrappedItemHandler.WriteMode.OUT);
    IItemHandlerModifiable automationFuel =
        new WrappedItemHandler(this.fuelInv, WrappedItemHandler.WriteMode.IN) {
          @NotNull
          @Override
          public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return SlotPredicates.FURNACE_FUEL.test(stack)
                ? super.insertItem(slot, stack, simulate)
                : stack;
          }
        };
    this.automationSides = new CombinedInvWrapper(automationFuel, this.automationOutput);
    this.joined =
        new CombinedInvWrapper(this.automationInput, automationFuel, this.automationOutput);
  }

  public void setLevel(@NotNull Level level) {
    super.setLevel(level);
    if (level instanceof ServerLevel serverLevel) {
      this.pullTarget =
          BlockCapabilityCache.create(
              Capabilities.ItemHandler.BLOCK,
              serverLevel,
              this.worldPosition.above(),
              Direction.DOWN);
      this.pushTarget =
          BlockCapabilityCache.create(
              Capabilities.ItemHandler.BLOCK,
              serverLevel,
              this.worldPosition.below(),
              Direction.UP);
    }
  }

  @Override
  protected boolean canProvideEmc() {
    return false;
  }

  @Override
  protected @Range(from = 0L, to = Long.MAX_VALUE) long getEmcInsertLimit() {
    return 2L;
  }

  protected int getInvSize() {
    return 9;
  }

  protected float getOreDoubleChance() {
    return 0.5F;
  }

  protected float getDoubleChance(ItemStack input) {
    if (input.is(Tags.Items.ORES)) {
      return this.getOreDoubleChance();
    } else {
      return input.is(Tags.Items.RAW_MATERIALS) ? this.getOreDoubleChance() * 2.0F / 3.0F : 0.0F;
    }
  }

  public float getBurnProgress() {
    if (this.cookingTotalTime == 0) {
      return 0.0F;
    }

    int progress =
        this.isLit() && this.canSmelt(this.getSmeltingRecipe(this.getItemToSmelt()))
            ? this.cookingProgress + 1
            : this.cookingProgress;
    return Mth.clamp((float) progress / this.cookingTotalTime, 0.0F, 1.0F);
  }

  @NotNull
  public AbstractContainerMenu createMenu(
      int windowId, @NotNull Inventory playerInv, @NotNull Player playerIn) {
    return new DMFurnaceContainer(windowId, playerInv, this);
  }

  @NotNull
  public Component getDisplayName() {
    return PELang.GUI_DARK_MATTER_FURNACE.translate();
  }

  public IItemHandler getFuel() {
    return this.fuelInv;
  }

  private ItemStack getItemToSmelt() {
    return this.inputInventory.getStackInSlot(0);
  }

  private ItemStack getFuelItem() {
    return this.fuelInv.getStackInSlot(0);
  }

  public IItemHandler getInput() {
    return this.inputInventory;
  }

  public IItemHandler getOutput() {
    return this.outputInventory;
  }

  @Override
  protected List<IItemHandlerModifiable> getInventoriesForDrops() {
    return List.of(this.inputInventory, this.fuelInv, this.outputInventory);
  }

  public static void tickServer(
      Level level, BlockPos pos, BlockState state, DMFurnaceBlockEntity furnace) {
    boolean wasBurning = furnace.isLit();
    int lastLitTime = furnace.litTime;
    int lastCookingProgress = furnace.cookingProgress;
    if (furnace.isLit()) {
      furnace.litTime--;
    }

    furnace.inputInventory.compact();
    furnace.outputInventory.compact();
    furnace.pullFromInventories(level, pos);
    DMFurnaceBlockEntity.RecipeResult recipeResult =
        furnace.getSmeltingRecipe(level, furnace.getItemToSmelt());
    boolean canSmelt = furnace.canSmelt(recipeResult);
    ItemStack fuelItem = furnace.getFuelItem();
    if (canSmelt) {
      IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(fuelItem, null);
      if (emcHolder != null) {
        long simulatedExtraction =
            emcHolder.extractEmc(fuelItem, 2L, IEmcStorage.EmcAction.SIMULATE);
        if (simulatedExtraction == 2L) {
          furnace.forceInsertEmc(
              emcHolder.extractEmc(fuelItem, simulatedExtraction, IEmcStorage.EmcAction.EXECUTE),
              IEmcStorage.EmcAction.EXECUTE);
        }

        furnace.markDirty(level, pos, false);
      }
    }

    if (furnace.getStoredEmc() >= 2L) {
      furnace.litTime = 1;
      furnace.forceExtractEmc(2L, IEmcStorage.EmcAction.EXECUTE);
    }

    if (canSmelt) {
      if (furnace.litTime == 0) {
        furnace.litDuration = furnace.litTime = furnace.getItemBurnTime(fuelItem);
        if (furnace.isLit() && !fuelItem.isEmpty()) {
          ItemStack copy = fuelItem.copy();
          fuelItem.shrink(1);
          furnace.fuelInv.onContentsChanged(0);
          if (fuelItem.isEmpty()) {
            ItemStackTemplate remainderTemplate = copy.getItem().getCraftingRemainder();
            furnace.fuelInv.setStackInSlot(
                0, remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create());
          }

          furnace.markDirty(level, pos, false);
        }
      }

      if (furnace.isLit() && ++furnace.cookingProgress == furnace.cookingTotalTime) {
        furnace.cookingProgress = 0;
        furnace.cookingTotalTime = furnace.getTotalCookTime(recipeResult);
        furnace.smeltItem(level, recipeResult);
      }
    } else {
      furnace.cookingProgress = 0;
    }

    if (wasBurning != furnace.isLit()) {
      if (state.getBlock() instanceof MatterFurnace) {
        level.setBlockAndUpdate(
            pos, (BlockState) state.setValue(MatterFurnace.LIT, furnace.isLit()));
      }

      furnace.markDirty(level, pos, true);
    }

    furnace.pushToInventories(level, pos);
    if (lastLitTime != furnace.litTime || lastCookingProgress != furnace.cookingProgress) {
      furnace.markDirty(level, pos, false);
    }

    furnace.updateComparators(level, pos);
  }

  public boolean isLit() {
    return this.litTime > 0;
  }

  private static boolean isHopper(@NotNull Level level, @NotNull BlockPos position) {
    return WorldHelper.getBlockEntity(level, position) instanceof Hopper;
  }

  private void pullFromInventories(@NotNull Level level, @NotNull BlockPos pos) {
    if (this.pullTarget != null && !isHopper(level, pos.above())) {
      IItemHandler handler = this.pullTarget.getCapability();
      if (handler != null) {
        int i = 0;

        for (int slots = handler.getSlots(); i < slots; i++) {
          ItemStack extractTest = handler.extractItem(i, Integer.MAX_VALUE, true);
          if (!extractTest.isEmpty()) {
            IItemHandler targetInv =
                SlotPredicates.FURNACE_FUEL.test(extractTest) ? this.fuelInv : this.inputInventory;
            this.transferItem(targetInv, i, extractTest, handler);
          }
        }
      }
    }
  }

  private void pushToInventories(@NotNull Level level, @NotNull BlockPos pos) {
    if (this.pushTarget != null
        && !this.outputInventory.isEmpty()
        && !isHopper(level, pos.below())) {
      IItemHandler targetInv = this.pushTarget.getCapability();
      if (targetInv != null) {
        int i = 0;

        for (int slots = this.outputInventory.getSlots(); i < slots; i++) {
          ItemStack extractTest = this.outputInventory.extractItem(i, Integer.MAX_VALUE, true);
          if (!extractTest.isEmpty()) {
            this.transferItem(targetInv, i, extractTest, this.outputInventory);
          }
        }
      }
    }
  }

  private void transferItem(
      IItemHandler targetInv, int i, ItemStack extractTest, IItemHandler outputInventory) {
    ItemStack remainderTest = ItemHandlerHelper.insertItemStacked(targetInv, extractTest, true);
    int successfullyTransferred = extractTest.getCount() - remainderTest.getCount();
    if (successfullyTransferred > 0) {
      ItemStack toInsert = outputInventory.extractItem(i, successfullyTransferred, false);
      ItemStack result = ItemHandlerHelper.insertItemStacked(targetInv, toInsert, false);
      assert result.isEmpty();
    }
  }

  private DMFurnaceBlockEntity.RecipeResult getSmeltingRecipe(ItemStack input) {
    return this.getSmeltingRecipe(this.level, input);
  }

  private DMFurnaceBlockEntity.RecipeResult getSmeltingRecipe(
      @Nullable Level level, ItemStack input) {
    if (level != null && !input.isEmpty()) {
      SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
      Optional<RecipeHolder<SmeltingRecipe>> optionalRecipe;
      if (level instanceof ServerLevel serverLevel) {
        optionalRecipe = this.quickCheck.getRecipeFor(recipeInput, serverLevel);
      } else {
        if (!(level.recipeAccess() instanceof RecipeManager recipeManager)) {
          return DMFurnaceBlockEntity.RecipeResult.EMPTY;
        }

        optionalRecipe = recipeManager.getRecipeFor(RecipeType.SMELTING, recipeInput, level);
      }

      if (optionalRecipe.isPresent()) {
        RecipeHolder<SmeltingRecipe> recipeHolder = optionalRecipe.get();
        return new DMFurnaceBlockEntity.RecipeResult(
            recipeHolder, ((SmeltingRecipe) recipeHolder.value()).assemble(recipeInput));
      } else {
        return DMFurnaceBlockEntity.RecipeResult.EMPTY;
      }
    } else {
      return DMFurnaceBlockEntity.RecipeResult.EMPTY;
    }
  }

  public boolean hasSmeltingResult(ItemStack input) {
    if (input.isEmpty() || this.level == null) {
      return false;
    } else {
      return this.level.isClientSide()
          ? this.level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(input)
          : this.getSmeltingRecipe(input).hasResult();
    }
  }

  private void smeltItem(
      @NotNull Level level, @NotNull DMFurnaceBlockEntity.RecipeResult recipeResult) {
    ItemStack toSmelt = this.getItemToSmelt();
    ItemStack smeltResult =
        recipeResult.scaledResult(level.getRandom(), this.getDoubleChance(toSmelt));
    if (!smeltResult.isEmpty()) {
      ItemHandlerHelper.insertItemStacked(this.outputInventory, smeltResult, false);
      if (toSmelt.is(Items.WET_SPONGE)) {
        ItemStack fuelItem = this.getFuelItem();
        if (!fuelItem.isEmpty() && fuelItem.is(Items.BUCKET)) {
          this.fuelInv.setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        }
      }

      toSmelt.shrink(1);
      this.inputInventory.onContentsChanged(0);
      this.setRecipeUsed(recipeResult.recipeHolder());
    }
  }

  private boolean canSmelt(DMFurnaceBlockEntity.RecipeResult recipeResult) {
    ItemStack smeltResult = recipeResult.result();
    if (smeltResult.isEmpty()) {
      return false;
    }

    ItemStack currentSmelted =
        this.outputInventory.getStackInSlot(this.outputInventory.getSlots() - 1);
    if (currentSmelted.isEmpty()) {
      return true;
    }

    if (!ItemStack.isSameItemSameComponents(smeltResult, currentSmelted)) {
      return false;
    }

    int result = currentSmelted.getCount() + smeltResult.getCount();
    return result <= currentSmelted.getMaxStackSize();
  }

  private int getItemBurnTime(ItemStack stack) {
    int burnDuration = this.level == null ? 0 : this.level.fuelValues().burnDuration(stack);
    return burnDuration * this.ticksBeforeSmelt / 200 * this.efficiencyBonus;
  }

  private int getTotalCookTime(DMFurnaceBlockEntity.RecipeResult recipeResult) {
    if (recipeResult.recipeHolder() == null) {
      return this.ticksBeforeSmelt;
    }

    int cookingTime = ((SmeltingRecipe) recipeResult.recipeHolder().value()).cookingTime();
    return Mth.ceil(this.ticksBeforeSmelt * cookingTime / 200.0F);
  }

  public float getLitProgress() {
    int litDuration = this.litDuration;
    if (litDuration == 0) {
      litDuration = this.ticksBeforeSmelt;
    }

    return Mth.clamp((float) this.litTime / litDuration, 0.0F, 1.0F);
  }

  @Override
  protected void loadAdditional(@NotNull ValueInput input) {
    super.loadAdditional(input);
    this.litTime = input.getIntOr("burn_time", 0);
    this.cookingProgress = input.getIntOr("cook_time", 0);
    this.cookingTotalTime = input.getIntOr("cook_time_total", 0);
    Provider registries = input.lookup();
    input
        .read("fuel", CompoundTag.CODEC)
        .ifPresent(tag -> this.fuelInv.deserializeNBT(registries, tag));
    input
        .read("input", CompoundTag.CODEC)
        .ifPresent(tag -> this.inputInventory.deserializeNBT(registries, tag));
    input
        .read("output", CompoundTag.CODEC)
        .ifPresent(tag -> this.outputInventory.deserializeNBT(registries, tag));
    input
        .read("recipes_used", Codec.unboundedMap(Identifier.CODEC, Codec.INT))
        .ifPresent(
            usedRecipes -> {
              for (java.util.Map.Entry<Identifier, Integer> entry : usedRecipes.entrySet()) {
                this.recipesUsed.put(entry.getKey(), entry.getValue());
              }
            });
    this.litDuration = this.getItemBurnTime(this.getFuelItem());
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput output) {
    super.saveAdditional(output);
    output.putInt("burn_time", this.litTime);
    output.putInt("cook_time", this.cookingProgress);
    output.putInt("cook_time_total", this.cookingTotalTime);
    Provider registries = this.level.registryAccess();
    output.store("input", CompoundTag.CODEC, this.inputInventory.serializeNBT(registries));
    output.store("output", CompoundTag.CODEC, this.outputInventory.serializeNBT(registries));
    output.store("fuel", CompoundTag.CODEC, this.fuelInv.serializeNBT(registries));
    Map<Identifier, Integer> usedRecipes = new HashMap<>();
    Iterator<Entry<Identifier>> iterator = Object2IntMaps.fastIterator(this.recipesUsed);

    while (iterator.hasNext()) {
      Entry<Identifier> entry = iterator.next();
      usedRecipes.put((Identifier) entry.getKey(), entry.getIntValue());
    }

    output.store("recipes_used", Codec.unboundedMap(Identifier.CODEC, Codec.INT), usedRecipes);
  }

  public void setRecipeUsed(@Nullable RecipeHolder<?> recipeHolder) {
    if (recipeHolder != null) {
      this.recipesUsed.addTo(recipeHolder.id().identifier(), 1);
    }
  }

  @Nullable
  public RecipeHolder<?> getRecipeUsed() {
    return null;
  }

  public void awardUsedRecipes(@NotNull Player player, @NotNull List<ItemStack> items) {}

  public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
    List<RecipeHolder<?>> recipes =
        this.getRecipesToAwardAndPopExperience(player.level(), player.position());
    player.awardRecipes(recipes);

    for (RecipeHolder<?> recipeholder : recipes) {
      player.triggerRecipeCrafted(recipeholder, Collections.emptyList());
    }

    this.recipesUsed.clear();
  }

  public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 popVec) {
    RecipeManager recipeManager = level.getServer().getRecipeManager();
    List<RecipeHolder<?>> list = new ArrayList<>();
    Iterator<Entry<Identifier>> iterator = Object2IntMaps.fastIterator(this.recipesUsed);

    while (iterator.hasNext()) {
      Entry<Identifier> entry = iterator.next();
      Optional<RecipeHolder<?>> optionalRecipe =
          recipeManager.byKey(ResourceKey.create(Registries.RECIPE, (Identifier) entry.getKey()));
      if (optionalRecipe.isPresent()) {
        RecipeHolder<?> recipeHolder = optionalRecipe.get();
        list.add(recipeHolder);
        if (recipeHolder.value() instanceof SmeltingRecipe recipe) {
          createExperience(level, popVec, entry.getIntValue(), recipe.experience());
        }
      }
    }

    return list;
  }

  private static void createExperience(
      ServerLevel level, Vec3 popVec, int recipeIndex, float experience) {
    float indexBasedExperience = recipeIndex * experience;
    int amount = Mth.floor(indexBasedExperience);
    float partial = indexBasedExperience - amount;
    if (partial != 0.0F && Math.random() < partial) {
      amount++;
    }

    ExperienceOrb.award(level, popVec, amount);
  }

  private record RecipeResult(
      @Nullable RecipeHolder<SmeltingRecipe> recipeHolder, ItemStack result) {
    private static final DMFurnaceBlockEntity.RecipeResult EMPTY =
        new DMFurnaceBlockEntity.RecipeResult(null, ItemStack.EMPTY);

    public ItemStack scaledResult(RandomSource random, float doubleChance) {
      return random.nextFloat() < doubleChance
          ? this.result.copyWithCount(2 * this.result.getCount())
          : this.result.copy();
    }

    public boolean hasResult() {
      return !this.result.isEmpty();
    }
  }
}
