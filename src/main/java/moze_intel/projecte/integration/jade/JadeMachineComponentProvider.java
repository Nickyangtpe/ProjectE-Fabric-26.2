package moze_intel.projecte.integration.jade;

import java.util.List;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public final class JadeMachineComponentProvider implements IBlockComponentProvider {
  static final JadeMachineComponentProvider INSTANCE = new JadeMachineComponentProvider();
  private static final int EMC_COLOR = 0xFFB04CFF;

  private JadeMachineComponentProvider() {}

  @Override
  public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    CompoundTag data = accessor.getServerData().getCompoundOrEmpty(PEJadeConstants.MACHINE_DATA);
    switch (data.getStringOr("Kind", "")) {
      case "pedestal" -> appendPedestal(tooltip, accessor, data);
      case "condenser" -> appendCondenser(tooltip, accessor, data);
      case "furnace" -> appendFurnace(tooltip, accessor, data);
      case "collector" -> appendCollector(tooltip, accessor, data);
      case "relay" -> appendRelay(tooltip, accessor, data);
      case "chest" ->
          tooltip.add(
              Component.translatable(
                      "jade.projecte.inventory",
                      data.getIntOr("ItemCount", 0),
                      data.getIntOr("UsedSlots", 0),
                      data.getIntOr("Slots", 0))
                  .withStyle(ChatFormatting.GRAY));
      case "emc" -> appendEmc(tooltip, data.getLongOr("Stored", 0), data.getLongOr("Capacity", 0));
      default -> {
      }
    }
  }

  @Override
  public Identifier getUid() {
    return PEJadeConstants.MACHINE_PROVIDER;
  }

  private static void appendPedestal(
      ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
    ItemStack item = getItem(data, "Item", accessor);
    if (item.isEmpty()) {
      tooltip.add(IThemeHelper.get().warning(Component.translatable("jade.projecte.pedestal.empty")));
      return;
    }
    addItemLine(
        tooltip, item, Component.translatable("jade.projecte.pedestal.item", item.getHoverName()));
    boolean active = data.getBooleanOr("Active", false);
    tooltip.add(
        active
            ? IThemeHelper.get().success(Component.translatable("jade.projecte.pedestal.active"))
            : IThemeHelper.get().danger(Component.translatable("jade.projecte.pedestal.inactive")));

    List<Component> description =
        data.read("Description", ComponentSerialization.CODEC.listOf()).orElse(List.of());
    for (Component line : description) {
      tooltip.add(line.copy().withStyle(ChatFormatting.AQUA));
    }

    int x = data.getIntOr("RangeX", 0);
    int y = data.getIntOr("RangeY", 0);
    int z = data.getIntOr("RangeZ", 0);
    if (x > 0 && y > 0 && z > 0) {
      int radius = Math.max(0, (Math.max(x, z) - 1) / 2);
      tooltip.add(
          Component.translatable("jade.projecte.pedestal.range", x, y, z, radius)
              .withStyle(ChatFormatting.LIGHT_PURPLE));
      tooltip.add(
          Component.translatable(
                  active
                      ? "jade.projecte.pedestal.range_preview.active"
                      : "jade.projecte.pedestal.range_preview.inactive")
              .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GOLD));
    }

    int cooldown = data.getIntOr("Cooldown", 0);
    if (active && cooldown > 0) {
      tooltip.add(
          Component.translatable(
                  "jade.projecte.pedestal.cooldown",
                  IThemeHelper.get().seconds(cooldown, accessor.tickRate()))
              .withStyle(ChatFormatting.GRAY));
    }
  }

  private static void appendCondenser(
      ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
    ItemStack target = getItem(data, "Target", accessor);
    long stored = data.getLongOr("Stored", 0);
    long required = data.getLongOr("Required", 0);
    if (target.isEmpty() || required <= 0) {
      tooltip.add(IThemeHelper.get().warning(Component.translatable("jade.projecte.condenser.no_target")));
      return;
    }
    addItemLine(tooltip, target, Component.translatable("jade.projecte.target", target.getHoverName()));
    appendProgress(
        tooltip,
        stored,
        required,
        Component.translatable(
            "jade.projecte.condenser.progress",
            EMCHelper.formatEmc(stored),
            EMCHelper.formatEmc(required)));
    ItemStack output = getItem(data, "Output", accessor);
    if (!output.isEmpty()) {
      addItemLine(tooltip, output, Component.translatable("jade.projecte.output", output.getHoverName()));
    }
  }

  private static void appendFurnace(
      ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
    appendEmc(tooltip, data.getLongOr("Stored", 0), data.getLongOr("Capacity", 0));
    ItemStack input = getItem(data, "Input", accessor);
    ItemStack fuel = getItem(data, "Fuel", accessor);
    ItemStack output = getItem(data, "Output", accessor);
    if (!input.isEmpty() || !fuel.isEmpty() || !output.isEmpty()) {
      tooltip.add(JadeUI.item(input).alignSelfCenter());
      if (!fuel.isEmpty()) {
        tooltip.append(JadeUI.smallItem(fuel).narration(""));
      }
      tooltip.append(JadeUI.progressArrow(Mth.clamp(data.getFloatOr("Progress", 0), 0, 1)));
      tooltip.append(JadeUI.item(output).alignSelfCenter());
    }
  }

  private static void appendCollector(
      ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
    appendEmc(tooltip, data.getLongOr("Stored", 0), data.getLongOr("Capacity", 0));
    appendItemIfPresent(tooltip, accessor, data, "Upgrading", "jade.projecte.upgrading");
    appendItemIfPresent(tooltip, accessor, data, "Output", "jade.projecte.output");
    appendItemIfPresent(tooltip, accessor, data, "Target", "jade.projecte.target");
  }

  private static void appendRelay(
      ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
    appendEmc(tooltip, data.getLongOr("Stored", 0), data.getLongOr("Capacity", 0));
    appendItemIfPresent(tooltip, accessor, data, "Input", "jade.projecte.burning");
    appendItemIfPresent(tooltip, accessor, data, "Charging", "jade.projecte.charging");
  }

  private static void appendItemIfPresent(
      ITooltip tooltip,
      BlockAccessor accessor,
      CompoundTag data,
      String key,
      String translation) {
    ItemStack stack = getItem(data, key, accessor);
    if (!stack.isEmpty()) {
      addItemLine(tooltip, stack, Component.translatable(translation, stack.getHoverName()));
    }
  }

  private static void appendEmc(ITooltip tooltip, long stored, long capacity) {
    if (capacity > 0) {
      appendProgress(
          tooltip,
          stored,
          capacity,
          Component.translatable(
              "jade.projecte.emc_storage",
              EMCHelper.formatEmc(stored),
              EMCHelper.formatEmc(capacity)));
    }
  }

  private static void appendProgress(
      ITooltip tooltip, long current, long maximum, Component text) {
    float progress = maximum <= 0 ? 0 : Mth.clamp((float) ((double) current / maximum), 0, 1);
    ProgressView view =
        new ProgressView(
            ProgressView.Part.of(progress, EMC_COLOR),
            text,
            JadeUI.progressStyle(),
            BoxStyle.nestedBox());
    tooltip.add(JadeUI.progress(view));
  }

  private static void addItemLine(ITooltip tooltip, ItemStack stack, Component text) {
    tooltip.add(JadeUI.smallItem(stack).narration(""));
    tooltip.append(JadeUI.spacer(3, 0));
    tooltip.append(text);
  }

  private static ItemStack getItem(
      CompoundTag data, String key, BlockAccessor accessor) {
    return data.contains(key)
        ? accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, data.get(key)).orElse(ItemStack.EMPTY)
        : ItemStack.EMPTY;
  }
}
