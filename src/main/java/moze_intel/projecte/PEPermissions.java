package moze_intel.projecte;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;

public final class PEPermissions {
  public static final PEPermissions.CommandPermissionNode COMMAND =
      node("command", Commands.LEVEL_ALL, true);
  public static final PEPermissions.CommandPermissionNode COMMAND_REMOVE_EMC =
      nodeOpCommand("remove_emc");
  public static final PEPermissions.CommandPermissionNode COMMAND_RESET_EMC =
      nodeOpCommand("reset_emc");
  public static final PEPermissions.CommandPermissionNode COMMAND_SET_EMC =
      nodeOpCommand("set_emc");
  public static final PEPermissions.CommandPermissionNode COMMAND_SHOW_BAG =
      nodeOpCommand("show_bag");
  public static final PEPermissions.CommandPermissionNode COMMAND_EMC = nodeOpCommand("emc");
  public static final PEPermissions.CommandPermissionNode COMMAND_EMC_ADD =
      nodeSubCommand(COMMAND_EMC, "add");
  public static final PEPermissions.CommandPermissionNode COMMAND_EMC_REMOVE =
      nodeSubCommand(COMMAND_EMC, "remove");
  public static final PEPermissions.CommandPermissionNode COMMAND_EMC_SET =
      nodeSubCommand(COMMAND_EMC, "set");
  public static final PEPermissions.CommandPermissionNode COMMAND_EMC_TEST =
      nodeSubCommand(COMMAND_EMC, "test");
  public static final PEPermissions.CommandPermissionNode COMMAND_EMC_GET =
      nodeSubCommand(COMMAND_EMC, "get");
  public static final PEPermissions.CommandPermissionNode COMMAND_KNOWLEDGE =
      nodeOpCommand("knowledge");
  public static final PEPermissions.CommandPermissionNode COMMAND_KNOWLEDGE_CLEAR =
      nodeSubCommand(COMMAND_KNOWLEDGE, "clear");
  public static final PEPermissions.CommandPermissionNode COMMAND_KNOWLEDGE_LEARN =
      nodeSubCommand(COMMAND_KNOWLEDGE, "learn");
  public static final PEPermissions.CommandPermissionNode COMMAND_KNOWLEDGE_UNLEARN =
      nodeSubCommand(COMMAND_KNOWLEDGE, "unlearn");
  public static final PEPermissions.CommandPermissionNode COMMAND_KNOWLEDGE_TEST =
      nodeSubCommand(COMMAND_KNOWLEDGE, "test");

  private PEPermissions() {}

  private static PEPermissions.CommandPermissionNode nodeOpCommand(String nodeName) {
    return node("command." + nodeName, Commands.LEVEL_GAMEMASTERS, false);
  }

  private static PEPermissions.CommandPermissionNode nodeSubCommand(
      PEPermissions.CommandPermissionNode parent, String nodeName) {
    return node(parent.nodeName + "." + nodeName, parent.fallback, true);
  }

  private static PEPermissions.CommandPermissionNode node(
      String nodeName, PermissionCheck fallback, boolean permissionDefault) {
    return new PEPermissions.CommandPermissionNode(
        nodeName, PermissionNode.of("projecte", nodeName), fallback, permissionDefault);
  }

  public record CommandPermissionNode(
      String nodeName,
      PermissionNode<Boolean> node,
      PermissionCheck fallback,
      boolean permissionDefault)
      implements Predicate<CommandSourceStack> {
    public boolean test(CommandSourceStack source) {
      return this.fallback.check(source.permissions())
          ? true
          : source.getPlayer() instanceof PermissionContextOwner permissionContext
              && (Boolean) permissionContext.checkPermission(this.node, this.permissionDefault);
    }
  }
}
