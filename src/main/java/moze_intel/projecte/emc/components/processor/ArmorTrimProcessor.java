package moze_intel.projecte.emc.components.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IComponentProcessorHelper;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.emc.components.DataComponentManager;
import moze_intel.projecte.mixin.SmithingTrimRecipeAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class ArmorTrimProcessor extends PersistentComponentProcessor<ArmorTrim> {
  private Map<Holder<TrimMaterial>, Long> materialEmc = Map.of();
  private Map<Holder<TrimPattern>, Long> templateEmc = Map.of();

  @Override
  public String getName() {
    return PEConfigTranslations.DCP_ARMOR_TRIM.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_ARMOR_TRIM.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_ARMOR_TRIM.tooltip();
  }

  public @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info,
      @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC,
      @NotNull ArmorTrim trim)
      throws ArithmeticException {
    long materialValue = this.materialEmc.getOrDefault(trim.material(), 0L);
    if (materialValue == 0L) {
      return 0L;
    }

    long templateValue = this.templateEmc.getOrDefault(trim.pattern(), 0L);
    return templateValue == 0L
        ? 0L
        : Math.addExact(Math.addExact(currentEMC, materialValue), templateValue);
  }

  @Override
  public void updateCachedValues(@Nullable ToLongFunction<ItemInfo> emcLookup) {
    if (emcLookup == null) {
      this.materialEmc = Map.of();
      this.templateEmc = Map.of();
    } else {
      Map<Holder<TrimMaterial>, Long> updatedMaterialEmc = new HashMap<>();

      for (Item item : BuiltInRegistries.ITEM) {
        Holder<TrimMaterial> material =
            (Holder<TrimMaterial>) new ItemStack(item).get(DataComponents.PROVIDES_TRIM_MATERIAL);
        if (material != null) {
          long value = emcLookup.applyAsLong(ItemInfo.fromItem(item));
          if (value > 0L) {
            updatedMaterialEmc.merge(material, value, Math::min);
          }
        }
      }

      Map<Holder<TrimPattern>, Long> updatedTemplateEmc = new HashMap<>();

      for (RecipeHolder<?> holder : DataComponentManager.getRecipeHolders()) {
        if (holder.value() instanceof SmithingTrimRecipe recipe) {
          Holder<TrimPattern> pattern = ((SmithingTrimRecipeAccessor) recipe).projecte$getPattern();
          Optional<Ingredient> templateIngredient = recipe.templateIngredient();
          if (templateIngredient.isPresent()) {
            long value =
                IComponentProcessorHelper.INSTANCE.getMinEmcFor(
                    emcLookup, templateIngredient.get());
            if (value > 0L) {
              updatedTemplateEmc.merge(pattern, value, Math::min);
            }
          }
        }
      }

      this.materialEmc = Map.copyOf(updatedMaterialEmc);
      this.templateEmc = Map.copyOf(updatedTemplateEmc);
    }
  }

  @Override
  protected boolean validItem(@NotNull ItemInfo info) {
    return info.getItem().is(ItemTags.TRIMMABLE_ARMOR);
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull ArmorTrim component) {
    return true;
  }

  @Override
  protected DataComponentType<ArmorTrim> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.TRIM;
  }
}
