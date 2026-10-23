package moze_intel.projecte.network.commands;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class SetEmcCMD {
  static final com.mojang.brigadier.exceptions.DynamicCommandExceptionType ERROR_INVALID_ITEM =
      new com.mojang.brigadier.exceptions.DynamicCommandExceptionType(
          item -> Component.translatableEscape("argument.item.id.invalid", item));

  private SetEmcCMD() {}

  public static LiteralArgumentBuilder<CommandSourceStack> register(CommandBuildContext context) {
    LiteralArgumentBuilder<CommandSourceStack> command =
        Commands.literal("setemc");

    // Original ProjectE syntax: /projecte setemc <emc> [item|#tag]
    command.then(
        Commands.argument("emc", LongArgumentType.longArg(0L, Long.MAX_VALUE))
            .then(itemArgument(context, "emc"))
            .then(tagArgument(context, "emc"))
            .executes(
                ctx ->
                    setEmc(
                        ctx,
                        RemoveEmcCMD.getHeldStack(ctx),
                        LongArgumentType.getLong(ctx, "emc"))));

    // Convenient item-first syntax: /projecte setemc <item> <emc>
    // This also avoids the confusing item.id.invalid message when the ID is entered first.
    command.then(
        Commands.argument("item", ItemArgument.item(context))
            .then(
                Commands.argument("item_emc", LongArgumentType.longArg(0L, Long.MAX_VALUE))
                    .executes(
                        ctx ->
                            setEmc(
                                ctx,
                                itemFromArgument(ctx, "item"),
                                LongArgumentType.getLong(ctx, "item_emc")))));

    return command;
  }

  private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> itemArgument(
      CommandBuildContext context, String emcArgument) {
    return Commands.argument("item", ItemArgument.item(context))
        .executes(
            ctx ->
                setEmc(
                    ctx,
                    itemFromArgument(ctx, "item"),
                    LongArgumentType.getLong(ctx, emcArgument)));
  }

  private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> tagArgument(
      CommandBuildContext context, String emcArgument) {
    return Commands.argument(
            "tag", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ITEM))
        .executes(
            ctx ->
                setEmc(
                    ctx,
                    tagFromArgument(ctx, "tag"),
                    LongArgumentType.getLong(ctx, emcArgument)));
  }

  private static NSSItem itemFromArgument(CommandContext<CommandSourceStack> ctx, String name)
      throws CommandSyntaxException {
    ItemInput input = ItemArgument.getItem(ctx, name);
    return NSSItem.createItem(input.createItemStack(1));
  }

  private static NSSItem tagFromArgument(CommandContext<CommandSourceStack> ctx, String name)
      throws CommandSyntaxException {
    Either<ResourceKey<Item>, TagKey<Item>> result =
        ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, name, Registries.ITEM, ERROR_INVALID_ITEM)
            .unwrap();
    return result.map(NSSItem::createItem, NSSItem::createTag);
  }

  private static int setEmc(CommandContext<CommandSourceStack> ctx, NSSItem toSet, long emc)
      throws CommandSyntaxException {
    CustomEMCParser.addToFile(toSet, emc);
    EmcCommandHelper.refresh(ctx.getSource());
    ctx.getSource().sendSuccess(() -> PELang.COMMAND_SET_SUCCESS.translate(toSet, emc), true);
    return 1;
  }
}
