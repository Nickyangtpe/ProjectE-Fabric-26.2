package moze_intel.projecte.network.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import moze_intel.projecte.PECore;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.commands.CommandSourceStack;

final class EmcCommandHelper {
  private static final SimpleCommandExceptionType REFRESH_FAILED =
      new SimpleCommandExceptionType(PELang.COMMAND_EMC_REFRESH_FAIL.translate());

  private EmcCommandHelper() {}

  static void refresh(CommandSourceStack source) throws CommandSyntaxException {
    try {
      PECore.refreshEMCValues(source.getServer());
    } catch (RuntimeException exception) {
      PECore.LOGGER.error("Failed to apply custom EMC command changes", exception);
      throw REFRESH_FAILED.create();
    }
  }
}
