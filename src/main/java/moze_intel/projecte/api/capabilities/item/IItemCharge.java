package moze_intel.projecte.api.capabilities.item;

import moze_intel.projecte.api.PEDataComponents;
import moze_intel.projecte.api.PESounds;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IItemCharge {
  int getNumCharges(@NotNull ItemStack var1);

  default float getChargePercent(@NotNull ItemStack stack) {
    return (float) this.getCharge(stack) / this.getNumCharges(stack);
  }

  default int getCharge(@NotNull ItemStack stack) {
    return Mth.clamp(
        (Integer) stack.getOrDefault(PEDataComponents.CHARGE, 0), 0, this.getNumCharges(stack));
  }

  default boolean changeCharge(
      @NotNull Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {
    return this.changeCharge(player, stack, hand, player.isSecondaryUseActive());
  }

  default boolean changeCharge(
      @NotNull Player player,
      @NotNull ItemStack stack,
      @Nullable InteractionHand hand,
      boolean decreaseCharge) {
    int currentCharge = this.getCharge(stack);
    int numCharges = this.getNumCharges(stack);
    if (decreaseCharge) {
      if (currentCharge > 0) {
        player
            .level()
            .playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                PESounds.UNCHARGE.value(),
                SoundSource.PLAYERS,
                1.0F,
                0.5F + 0.5F / numCharges * currentCharge);
        stack.set(PEDataComponents.CHARGE, currentCharge - 1);
        PlayerHelper.sendOverlayMessage(
            player,
            Component.translatable("charge.projecte.level", currentCharge - 1, numCharges));
        return true;
      }
    } else if (currentCharge < numCharges) {
      player
          .level()
          .playSound(
              null,
              player.getX(),
              player.getY(),
              player.getZ(),
              PESounds.CHARGE.value(),
              SoundSource.PLAYERS,
              1.0F,
              0.5F + 0.5F / numCharges * currentCharge);
      stack.set(PEDataComponents.CHARGE, currentCharge + 1);
      PlayerHelper.sendOverlayMessage(
          player,
          Component.translatable("charge.projecte.level", currentCharge + 1, numCharges));
      return true;
    }

    return false;
  }
}
