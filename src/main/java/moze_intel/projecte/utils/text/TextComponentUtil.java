package moze_intel.projecte.utils.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import moze_intel.projecte.compat.fluids.FluidStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class TextComponentUtil {
  private static final Component TEXT_NULL = getString("null");

  private TextComponentUtil() {}

  public static MutableComponent build(Object... components) {
    MutableComponent result = null;
    Style cachedStyle = Style.EMPTY;

    for (Object component : components) {
      if (component != null) {
        MutableComponent current = null;
        switch (component) {
          case IHasTextComponent hasTextComponent:
            current = hasTextComponent.getTextComponent().copy();
            break;
          case IHasTranslationKey hasTranslationKey:
            current = translate(hasTranslationKey.getTranslationKey());
            break;
          case TextColor color:
            cachedStyle = cachedStyle.withColor(color);
            break;
          case Component c:
            current = c.copy();
            break;
          case ChatFormatting formatting:
            cachedStyle = cachedStyle.applyFormat(formatting);
            break;
          case ClickEvent event:
            cachedStyle = cachedStyle.withClickEvent(event);
            break;
          case HoverEvent event:
            cachedStyle = cachedStyle.withHoverEvent(event);
            break;
          case Block block:
            current = block.getName().copy();
            break;
          case Item item:
            current = item.getName(new ItemStack(item)).copy();
            break;
          case ItemStack stack:
            current = stack.getHoverName().copy();
            break;
          case FluidStack stack:
            current = stack.getHoverName().copy();
            break;
          case Fluid fluid:
            current = getFluidName(fluid);
            break;
          case EntityType<?> entityType:
            current = entityType.getDescription().copy();
            break;
          case Level level:
            current = getLevelName(level);
            break;
          default:
            current = getString(component.toString());
        }

        if (current != null) {
          if (!cachedStyle.isEmpty()) {
            current.setStyle(cachedStyle);
            cachedStyle = Style.EMPTY;
          }

          if (result == null) {
            result = current;
          } else {
            result.append(current);
          }
        }
      }
    }

    return result;
  }

  public static MutableComponent getString(String component) {
    return Component.literal(cleanString(component));
  }

  private static String cleanString(String component) {
    return component.replace(" ", " ");
  }

  public static MutableComponent translate(String key) {
    return Component.translatable(key);
  }

  public static MutableComponent translate(String key, Object... args) {
    return Component.translatable(key, args);
  }

  public static MutableComponent smartTranslate(String key, Object... components) {
    if (components.length == 0) {
      return translate(key);
    }

    List<Object> args = new ArrayList<>();
    Style cachedStyle = Style.EMPTY;

    for (Object component : components) {
      if (component == null) {
        args.add(TEXT_NULL);
        cachedStyle = Style.EMPTY;
      } else {
        MutableComponent current = null;
        if (component instanceof Component c) {
          current = c.copy();
        } else if (component instanceof IHasTextComponent hasTextComponent) {
          current = hasTextComponent.getTextComponent().copy();
        } else if (component instanceof IHasTranslationKey hasTranslationKey) {
          current = translate(hasTranslationKey.getTranslationKey());
        } else if (component instanceof Block block) {
          current = block.getName().copy();
        } else if (component instanceof Item item) {
          current = item.getName(new ItemStack(item)).copy();
        } else if (component instanceof ItemStack stack) {
          current = stack.getHoverName().copy();
        } else if (component instanceof FluidStack stack) {
          current = stack.getHoverName().copy();
        } else if (component instanceof Fluid fluid) {
          current = getFluidName(fluid);
        } else if (component instanceof EntityType<?> entityType) {
          current = entityType.getDescription().copy();
        } else if (component instanceof Level level) {
          current = getLevelName(level);
        } else {
          if (component instanceof TextColor color && cachedStyle.getColor() == null) {
            cachedStyle = cachedStyle.withColor(color);
            continue;
          }

          if (component instanceof ChatFormatting formatting
              && !hasStyleType(cachedStyle, formatting)) {
            cachedStyle = cachedStyle.applyFormat(formatting);
            continue;
          }

          if (component instanceof ClickEvent event && cachedStyle.getClickEvent() == null) {
            cachedStyle = cachedStyle.withClickEvent(event);
            continue;
          }

          if (component instanceof HoverEvent event && cachedStyle.getHoverEvent() == null) {
            cachedStyle = cachedStyle.withHoverEvent(event);
            continue;
          }

          if (!cachedStyle.isEmpty()) {
            current = getString(component.toString());
          } else if (component instanceof String str) {
            component = cleanString(str);
          } else if (!TranslatableContents.isAllowedPrimitiveArgument(component)) {
            component = cleanString(component.toString());
          }
        }

        if (!cachedStyle.isEmpty()) {
          if (current == null) {
            current = getString(component.toString());
          }

          args.add(current.setStyle(cachedStyle));
          cachedStyle = Style.EMPTY;
        } else {
          args.add(Objects.requireNonNullElse(current, component));
        }
      }
    }

    if (!cachedStyle.isEmpty()) {
      Object lastComponent = components[components.length - 1];
      if (lastComponent == null) {
        args.add(TEXT_NULL);
      } else if (!(lastComponent instanceof Component)
          && !TranslatableContents.isAllowedPrimitiveArgument(lastComponent)) {
        args.add(cleanString(lastComponent.toString()));
      } else {
        args.add(lastComponent);
      }
    }

    return translate(key, args.toArray());
  }

  private static MutableComponent getFluidName(Fluid fluid) {
    Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
    return Component.translatable(id.toLanguageKey("fluid_type"));
  }

  private static MutableComponent getLevelName(Level level) {
    Identifier id = level.dimension().identifier();
    return Component.translatableWithFallback(id.toLanguageKey("dimension"), id.toString());
  }

  private static boolean hasStyleType(Style current, ChatFormatting formatting) {
    return switch (formatting) {
      case OBFUSCATED -> current.isObfuscated();
      case BOLD -> current.isBold();
      case STRIKETHROUGH -> current.isStrikethrough();
      case UNDERLINE -> current.isUnderlined();
      case ITALIC -> current.isItalic();
      case RESET -> current.isEmpty();
      default -> current.getColor() != null;
    };
  }
}
