package moze_intel.projecte.emc.components.processor;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.function.LongSupplier;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class EnchantmentProcessor extends PersistentComponentProcessor<ItemEnchantments> {
  private static final ResourceKey<Item> ENCHANTED_BOOK =
      (ResourceKey<Item>) BuiltInRegistries.ITEM.getResourceKey(Items.ENCHANTED_BOOK).orElseThrow();
  private static final long DEFAULT_ENCHANT_EMC_BONUS = 5000L;
  private LongSupplier enchantmentEmcBonus = () -> 5000L;

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_ENCHANTMENT.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_ENCHANTMENT.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_ENCHANTMENT.tooltip();
  }

  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public boolean usePersistentComponents() {
    return false;
  }

  @Override
  public void addConfigOptions(ModConfigSpec.Builder configBuilder) {
    ModConfigSpec.LongValue value =
        PEConfigTranslations.DCP_ENCHANTMENT_EMC_BONUS
            .applyToBuilder(configBuilder)
            .worldRestart()
            .defineInRange("enchantment_emc_bonus", 5000L, 0L, Long.MAX_VALUE);
    this.enchantmentEmcBonus = value::get;
  }

  public @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info,
      @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC,
      @NotNull ItemEnchantments enchantments)
      throws ArithmeticException {
    long emcBonus = this.enchantmentEmcBonus.getAsLong();

    for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
      int rarityWeight = ((Enchantment) ((Holder) entry.getKey()).value()).definition().weight();
      if (rarityWeight > 0) {
        currentEMC =
            Math.addExact(
                currentEMC, Math.multiplyExact(emcBonus / rarityWeight, entry.getIntValue()));
      }
    }

    return currentEMC;
  }

  @Override
  protected DataComponentType<ItemEnchantments> getComponentType(@NotNull ItemInfo info) {
    return info.getItem().is(ENCHANTED_BOOK)
        ? DataComponents.STORED_ENCHANTMENTS
        : DataComponents.ENCHANTMENTS;
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull ItemEnchantments component) {
    return !component.isEmpty();
  }
}
