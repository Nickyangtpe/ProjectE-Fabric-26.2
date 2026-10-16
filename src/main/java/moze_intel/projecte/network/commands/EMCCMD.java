package moze_intel.projecte.network.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class EMCCMD {
  public static ArgumentBuilder<CommandSourceStack, ?> register(CommandBuildContext context) {
    LiteralArgumentBuilder<CommandSourceStack> command =
        Commands.literal("emc");

    LiteralArgumentBuilder<CommandSourceStack> add =
        Commands.literal("add");
    add.then(executeWithParameters(ActionType.ADD));
    command.then(add);

    LiteralArgumentBuilder<CommandSourceStack> remove =
        Commands.literal("remove");
    remove.then(executeWithParameters(ActionType.REMOVE));
    command.then(remove);

    LiteralArgumentBuilder<CommandSourceStack> set =
        Commands.literal("set");
    set.then(executeWithParameters(ActionType.SET));
    command.then(set);

    LiteralArgumentBuilder<CommandSourceStack> test =
        Commands.literal("test");
    test.then(executeWithParameters(ActionType.TEST));
    command.then(test);

    LiteralArgumentBuilder<CommandSourceStack> get =
        Commands.literal("get");
    get.then(
        Commands.argument("player", EntityArgument.player())
            .executes(ctx -> handle(ctx, ActionType.GET)));
    command.then(get);
    return command;
  }

  private static ArgumentBuilder<CommandSourceStack, ?> executeWithParameters(
      EMCCMD.ActionType actionType) {
    return Commands.argument("player", EntityArgument.player())
        .then(
            Commands.argument("value", StringArgumentType.string())
                .executes(ctx -> handle(ctx, actionType)));
  }

  private static MutableComponent formatEMC(BigInteger emc) {
    return TextComponentUtil.build(ChatFormatting.GRAY, TransmutationEMCFormatter.formatEMC(emc));
  }

  private static int handle(CommandContext<CommandSourceStack> ctx, EMCCMD.ActionType action)
      throws CommandSyntaxException {
    CommandSourceStack source = ctx.getSource();
    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
    IKnowledgeProvider provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    if (provider == null) {
      source.sendFailure(PELang.COMMAND_PROVIDER_FAIL.translate(player.getDisplayName()));
      return 0;
    }

    if (action == EMCCMD.ActionType.GET) {
      source.sendSuccess(
          () ->
              PELang.COMMAND_EMC_GET_SUCCESS.translate(
                  player.getDisplayName(), formatEMC(provider.getEmc())),
          true);
      return 1;
    }

    String val = StringArgumentType.getString(ctx, "value");
    BigInteger value = null;

    try {
      value = new BigInteger(val);
      if (value.signum() == -1) {
        switch (action) {
          case ADD:
          case REMOVE:
            action =
                action == EMCCMD.ActionType.ADD ? EMCCMD.ActionType.REMOVE : EMCCMD.ActionType.ADD;
            value = value.abs();
            break;
          case SET:
          case TEST:
            value = null;
          case GET:
        }
      }
    } catch (NumberFormatException var9) {
    }

    if (value == null) {
      source.sendFailure(PELang.COMMAND_EMC_INVALID.translate(val));
      return 0;
    }

    BigInteger newEMC = provider.getEmc();
    switch (action) {
      case ADD:
        {
          newEMC = newEMC.add(value);
          Component message =
              PELang.COMMAND_EMC_ADD_SUCCESS.translate(
                  formatEMC(value), player.getDisplayName(), formatEMC(newEMC));
          source.sendSuccess(() -> message, true);
          break;
        }
      case REMOVE:
        {
          newEMC = newEMC.subtract(value);
          if (newEMC.signum() == -1) {
            source.sendFailure(
                PELang.COMMAND_EMC_NEGATIVE.translate(formatEMC(value), player.getDisplayName()));
            return 0;
          }

          Component message =
              PELang.COMMAND_EMC_REMOVE_SUCCESS.translate(
                  formatEMC(value), player.getDisplayName(), formatEMC(newEMC));
          source.sendSuccess(() -> message, true);
          break;
        }
      case SET:
        {
          newEMC = value;
          Component message =
              PELang.COMMAND_EMC_SET_SUCCESS.translate(player.getDisplayName(), formatEMC(value));
          source.sendSuccess(() -> message, true);
        }
      case GET:
      default:
        break;
      case TEST:
        if (newEMC.compareTo(value) >= 0) {
          Component messagex =
              PELang.COMMAND_EMC_TEST_SUCCESS.translateColored(
                  ChatFormatting.GREEN, player.getDisplayName(), formatEMC(value));
          source.sendSuccess(() -> messagex, true);
          return 1;
        }

        source.sendFailure(
            PELang.COMMAND_EMC_TEST_FAIL.translate(player.getDisplayName(), formatEMC(value)));
        return 0;
    }

    provider.setEmc(newEMC);
    provider.syncEmc(player);
    return 1;
  }

  private enum ActionType {
    ADD,
    REMOVE,
    SET,
    GET,
    TEST;
  }
}
