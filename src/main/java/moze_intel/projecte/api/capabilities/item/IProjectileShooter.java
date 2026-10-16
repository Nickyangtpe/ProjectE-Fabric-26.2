package moze_intel.projecte.api.capabilities.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IProjectileShooter {
  boolean shootProjectile(
      @NotNull Player var1, @NotNull ItemStack var2, @Nullable InteractionHand var3);
}
