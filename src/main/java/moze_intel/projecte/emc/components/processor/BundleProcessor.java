package moze_intel.projecte.emc.components.processor;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.jetbrains.annotations.NotNull;

@DataComponentProcessor
public class BundleProcessor extends SimpleContainerProcessor<BundleContents> {
  @Override
  public String getName() {
    return PEConfigTranslations.DCP_BUNDLE.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_BUNDLE.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_BUNDLE.tooltip();
  }

  @Override
  protected DataComponentType<BundleContents> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.BUNDLE_CONTENTS;
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull BundleContents component) {
    return !component.isEmpty();
  }

  protected Iterable<ItemStack> getStoredItems(BundleContents component) {
    return component.items().stream().<ItemStack>map(ItemStackTemplate::create).toList();
  }
}
