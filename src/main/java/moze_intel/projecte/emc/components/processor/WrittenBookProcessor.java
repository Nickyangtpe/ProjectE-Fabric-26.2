package moze_intel.projecte.emc.components.processor;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class WrittenBookProcessor extends SimplePersistentComponentProcessor<WrittenBookContent> {
  @Override
  public String getName() {
    return PEConfigTranslations.DCP_WRITTEN_BOOK.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.DCP_WRITTEN_BOOK.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.DCP_WRITTEN_BOOK.tooltip();
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long recalculateEMC(
      @NotNull ItemInfo info, @Range(from = 1L, to = Long.MAX_VALUE) long currentEMC)
      throws ArithmeticException {
    return currentEMC;
  }

  @Override
  protected DataComponentType<WrittenBookContent> getComponentType(@NotNull ItemInfo info) {
    return DataComponents.WRITTEN_BOOK_CONTENT;
  }

  protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull WrittenBookContent component) {
    return !component.equals(WrittenBookContent.EMPTY);
  }
}
