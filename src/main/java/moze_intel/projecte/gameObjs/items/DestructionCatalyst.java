package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DestructionCatalyst extends ItemPE implements IItemCharge, IBarHelper {
  public DestructionCatalyst(Properties props) {
    super(
        props
            .component(PEDataComponentTypes.CHARGE, 0)
            .component(PEDataComponentTypes.STORED_EMC, 0L));
  }

  @NotNull
  public InteractionResult useOn(UseOnContext ctx) {
    Player player = ctx.getPlayer();
    if (player == null) {
      return InteractionResult.FAIL;
    }

    Level level = ctx.getLevel();
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    ItemStack stack = ctx.getItemInHand();
    int numRows = this.calculateDepthFromCharge(stack);
    boolean hasAction = false;
    List<ItemStack> drops = new ArrayList<>();
    Vec3 dropPosition = Vec3.atLowerCornerOf(ctx.getClickedPos());

    for (BlockPos pos :
        WorldHelper.getPositionsInBox(
            WorldHelper.getDeepBox(ctx.getClickedPos(), ctx.getClickedFace(), --numRows))) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir()) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness != -1.0F && !(hardness >= 50.0F)) {
          if (!consumeFuel(player, stack, 8L, true)) {
            break;
          }

          hasAction = true;
          pos = pos.immutable();
          if (PlayerHelper.hasBreakPermission((ServerPlayer) player, level, pos)) {
            BlockEntity blockEntity = WorldHelper.getBlockEntity(level, pos);
            if (!level.removeBlock(pos, false)) {
              continue;
            }
            WorldHelper.collectBlockDrops(
                state, (ServerLevel) level, pos, blockEntity, player, stack, drops, dropPosition);
            if (level.getRandom().nextInt(8) == 0) {
              ((ServerLevel) level)
                  .sendParticles(
                      level.getRandom().nextBoolean()
                          ? ParticleTypes.POOF
                          : ParticleTypes.LARGE_SMOKE,
                      pos.getX(),
                      pos.getY(),
                      pos.getZ(),
                      2,
                      0.0,
                      0.0,
                      0.0,
                      0.05);
            }
          }
        }
      }
    }

    if (hasAction) {
      WorldHelper.createLootDrop(drops, level, dropPosition);
      level.playSound(
          null,
          player.getX(),
          player.getY(),
          player.getZ(),
          PESoundEvents.DESTRUCT.get(),
          SoundSource.PLAYERS,
          1.0F,
          1.0F);
    }

    return InteractionResult.CONSUME;
  }

  protected int calculateDepthFromCharge(ItemStack stack) {
    int charge = this.getCharge(stack);
    return charge == 0 ? 1 : (int) Math.pow(2.0, 1 + charge);
  }

  @Override
  public int getNumCharges(@NotNull ItemStack stack) {
    return 3;
  }

  public boolean isBarVisible(@NotNull ItemStack stack) {
    return true;
  }

  @Override
  public float getWidthForBar(ItemStack stack) {
    return 1.0F - this.getChargePercent(stack);
  }

  public int getBarWidth(@NotNull ItemStack stack) {
    return this.getScaledBarWidth(stack);
  }

  public int getBarColor(@NotNull ItemStack stack) {
    return this.getColorForBar(stack);
  }
}
