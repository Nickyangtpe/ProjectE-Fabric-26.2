package moze_intel.projecte.network.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RemoveEmcCMD {
  private static final SimpleCommandExceptionType EMPTY_STACK =
      new SimpleCommandExceptionType(PELang.COMMAND_NO_ITEM.translate());

  public static LiteralArgumentBuilder<CommandSourceStack> register(CommandBuildContext context) {
    LiteralArgumentBuilder<CommandSourceStack> command =
        Commands.literal("removeemc");
    command.then(
        Commands.argument("item", ItemArgument.item(context))
            .executes(
                ctx -> {
                  ItemInput itemInput = ItemArgument.getItem(ctx, "item");
                  return removeEmc(ctx, NSSItem.createItem(itemInput.createItemStack(1)));
                }));
    command.then(
        Commands.argument("tag", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ITEM))
            .executes(
                ctx -> {
                  Either<ResourceKey<Item>, TagKey<Item>> result =
                      ResourceOrTagKeyArgument.getResourceOrTagKey(
                              ctx, "tag", Registries.ITEM, SetEmcCMD.ERROR_INVALID_ITEM)
                          .unwrap();
                  return removeEmc(ctx, result.map(NSSItem::createItem, NSSItem::createTag));
                }));
    command.executes(ctx -> removeEmc(ctx, getHeldStack(ctx)));
    return command;
  }

  private static int removeEmc(CommandContext<CommandSourceStack> ctx, NSSItem item)
      throws CommandSyntaxException {
    CustomEMCParser.addToFile(item, 0L);
    EmcCommandHelper.refresh(ctx.getSource());
    ctx.getSource()
        .sendSuccess(() -> PELang.COMMAND_REMOVE_SUCCESS.translate(item), true);
    return 1;
  }

  public static NSSItem getHeldStack(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ItemStack stack = player.getMainHandItem();
    if (stack.isEmpty()) {
      stack = player.getOffhandItem();
    }

    if (stack.isEmpty()) {
      throw EMPTY_STACK.create();
    } else {
      return NSSItem.createItem(stack);
    }
  }
}
