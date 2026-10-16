package moze_intel.projecte.compat.util;

import net.minecraft.world.InteractionResult;

public final class PESidedSuccess {
  private PESidedSuccess() {}

  public static InteractionResult sidedSuccess(boolean isClientSide) {
    return isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
  }
}
