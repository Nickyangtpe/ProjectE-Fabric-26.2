package moze_intel.projecte.integration.trinkets;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.wrapper.CombinedInvWrapper;
import moze_intel.projecte.integration.curios.IExposesCurioAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TrinketsCompat {
  public static final String TABLET_SLOT = "projecte/transmutation_tablet";
  private static final TagKey<Item> TABLET_ITEMS =
      TagKey.create(
          Registries.ITEM,
          Identifier.fromNamespaceAndPath("trinkets", "projecte/transmutation_tablet"));
  private static final TrinketCallback PROJECT_E_CALLBACK =
      new TrinketsCompat.ProjectETrinketCallback();

  private TrinketsCompat() {}

  public static void registerItem(Item item) {
    TrinketCallback.setCallback(item, PROJECT_E_CALLBACK);
  }

  public static Optional<IItemHandler> getTabletInventory(Player player) {
    TrinketAttachment attachment = TrinketsApi.getAttachment(player);
    TrinketInventory inventory = attachment.getInventory("projecte/transmutation_tablet");
    return Optional.ofNullable(inventory)
        .map(value -> new TrinketsCompat.TrinketInventoryHandler(value, TABLET_ITEMS));
  }

  @Nullable
  public static IItemHandler getAllAccessoryInventories(Player player) {
    Map<String, TrinketInventory> inventories = TrinketsApi.getAttachment(player).getInventories();
    if (inventories.isEmpty()) {
      return null;
    }

    List<Entry<String, TrinketInventory>> entries = new ArrayList<>(inventories.entrySet());
    entries.sort(Comparator.comparing(Entry::getKey));
    IItemHandlerModifiable[] handlers = new IItemHandlerModifiable[entries.size()];

    for (int i = 0; i < entries.size(); i++) {
      handlers[i] = new TrinketsCompat.TrinketInventoryHandler(entries.get(i).getValue(), null);
    }

    return handlers.length == 1 ? handlers[0] : new CombinedInvWrapper(handlers);
  }

  private static final class ProjectETrinketCallback implements TrinketCallback {
    public void tick(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
      if (!slot.cosmetic() && slot.canApplyEffects()) {
        stack.inventoryTick(entity.level(), entity, EquipmentSlot.OFFHAND);
      }
    }

    public void forEachTrinketModifier(
        ItemStack stack,
        TrinketSlotAccess slot,
        LivingEntity entity,
        Identifier slotIdentifier,
        BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
      if (!slot.cosmetic()
          && slot.canApplyEffects()
          && stack.getItem() instanceof IExposesCurioAttributes exposes) {
        Multimap<Holder<Attribute>, AttributeModifier> attributes = LinkedHashMultimap.create();
        exposes.addAttributes(attributes);
        attributes.forEach(consumer);
      }
    }
  }

  private static final class TrinketInventoryHandler implements IItemHandlerModifiable {
    private final TrinketInventory inventory;
    @Nullable private final TagKey<Item> allowedItems;

    private TrinketInventoryHandler(
        TrinketInventory inventory, @Nullable TagKey<Item> allowedItems) {
      this.inventory = inventory;
      this.allowedItems = allowedItems;
    }

    @Override
    public int getSlots() {
      return this.inventory.getContainerSize();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
      this.checkSlot(slot);
      return this.inventory.getItem(slot);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
      this.checkSlot(slot);
      this.inventory.setItem(slot, stack);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
      this.checkSlot(slot);
      if (stack.isEmpty()) {
        return ItemStack.EMPTY;
      }

      if (!this.isItemValid(slot, stack)) {
        return stack;
      }

      ItemStack existing = this.inventory.getItem(slot);
      if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
        return stack;
      }

      int currentCount = existing.getCount();
      int limit = this.inventory.getMaxStackSize(stack);
      int accepted = Math.min(stack.getCount(), Math.max(0, limit - currentCount));
      if (accepted == 0) {
        return stack;
      }

      if (!simulate) {
        ItemStack replacement =
            existing.isEmpty()
                ? stack.copyWithCount(accepted)
                : existing.copyWithCount(currentCount + accepted);
        this.inventory.setItem(slot, replacement);
      }

      return accepted == stack.getCount()
          ? ItemStack.EMPTY
          : stack.copyWithCount(stack.getCount() - accepted);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      this.checkSlot(slot);
      if (amount <= 0) {
        return ItemStack.EMPTY;
      }

      ItemStack existing = this.inventory.getItem(slot);
      if (existing.isEmpty()) {
        return ItemStack.EMPTY;
      }

      int extracted = Math.min(amount, existing.getCount());
      ItemStack result = existing.copyWithCount(extracted);
      if (!simulate) {
        this.inventory.setItem(
            slot,
            extracted == existing.getCount()
                ? ItemStack.EMPTY
                : existing.copyWithCount(existing.getCount() - extracted));
      }

      return result;
    }

    @Override
    public int getSlotLimit(int slot) {
      this.checkSlot(slot);
      return this.inventory.getMaxStackSize(ItemStack.EMPTY);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
      this.checkSlot(slot);
      return this.inventory.isValidSlot(slot)
          && (this.allowedItems == null || stack.is(this.allowedItems))
          && this.inventory.canPlaceItem(slot, stack);
    }

    private void checkSlot(int slot) {
      if (slot < 0 || slot >= this.inventory.getContainerSize()) {
        throw new IndexOutOfBoundsException(
            "Slot " + slot + " not in range [0," + this.inventory.getContainerSize() + ")");
      }
    }
  }
}
