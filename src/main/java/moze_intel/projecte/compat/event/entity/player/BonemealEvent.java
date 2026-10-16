package moze_intel.projecte.compat.event.entity.player;

import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.ICancellableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BonemealEvent extends Event implements ICancellableEvent {
  private final Player player;
  private final Level level;
  private final BlockPos pos;
  private final BlockState state;
  private final ItemStack stack;
  private final boolean validBonemealTarget;
  private boolean successful;

  public BonemealEvent(
      Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
    this.player = player;
    this.level = level;
    this.pos = pos.immutable();
    this.state = state;
    this.stack = stack;
    this.validBonemealTarget =
        state.getBlock() instanceof BonemealableBlock growable
            && growable.isValidBonemealTarget(level, pos, state);
  }

  public Player getPlayer() {
    return this.player;
  }

  public Level getLevel() {
    return this.level;
  }

  public BlockPos getPos() {
    return this.pos;
  }

  public BlockState getState() {
    return this.state;
  }

  public ItemStack getItemStack() {
    return this.stack;
  }

  public boolean isValidBonemealTarget() {
    return this.validBonemealTarget;
  }

  public boolean isSuccessful() {
    return this.successful;
  }

  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }
}
