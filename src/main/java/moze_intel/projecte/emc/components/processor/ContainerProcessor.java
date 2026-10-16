package moze_intel.projecte.emc.components.processor;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.NotNull;

@DataComponentProcessor
public class ContainerProcessor extends SimpleContainerProcessor<ItemContainerContents> {
  @Override
  public String getName() {
    return PEConfigTranslations.DCP_CONTAINER.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_CONTAINER.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_CONTAINER.tooltip();
  }

  @Override
  protected DataComponentType<ItemContainerContents> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.CONTAINER;
  }

  protected boolean shouldPersist(
      @NotNull ItemInfo info, @NotNull ItemContainerContents component) {
    return !component.equals(ItemContainerContents.EMPTY);
  }

  protected Iterable<ItemStack> getStoredItems(ItemContainerContents component) {
    List<ItemStack> items = new ArrayList<>();
    component.nonEmptyItems().forEach(template -> items.add(template.create()));
    return items;
  }
}
