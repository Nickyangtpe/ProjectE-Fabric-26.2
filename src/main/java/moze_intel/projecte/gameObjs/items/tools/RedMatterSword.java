package moze_intel.projecte.gameObjs.items.tools;

import java.util.function.Consumer;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.items.IItemMode;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

public class RedMatterSword extends PESword implements IItemMode<PEKatar.KatarMode> {
  public RedMatterSword(Properties props) {
    super(
        EnumMatterType.RED_MATTER,
        3,
        12,
        props.component(PEDataComponentTypes.KATAR_MODE.get(), PEKatar.KatarMode.SLAY_HOSTILE));
  }

  @Override
  protected boolean slayAll(@NotNull ItemStack stack) {
    return this.getMode(stack) == PEKatar.KatarMode.SLAY_ALL;
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(this.getToolTip(stack));
  }

  @Override
  public DataComponentType<PEKatar.KatarMode> getDataComponentType() {
    return PEDataComponentTypes.KATAR_MODE.get();
  }

  public PEKatar.KatarMode getDefaultMode() {
    return PEKatar.KatarMode.SLAY_HOSTILE;
  }
}
