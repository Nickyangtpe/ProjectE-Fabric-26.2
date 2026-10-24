package moze_intel.projecte.gameObjs.items.rings;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IPedestalRangeItem;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.RegistryUtils;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.ILangEntry;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class TimeWatch extends PEToggleItem
    implements IPedestalRangeItem, IItemCharge, IBarHelper {
  private static final Predicate<BlockEntity> VALID_TARGET =
      be ->
          !be.isRemoved()
              && be.hasLevel()
              && !RegistryUtils.getBEHolder(be.getType())
                  .is(PETags.BlockEntities.BLACKLIST_TIME_WATCH);

  public TimeWatch(Properties props) {
    super(
        props
            .component(PEDataComponentTypes.CHARGE, 0)
            .component(PEDataComponentTypes.STORED_EMC, 0L)
            .component(PEDataComponentTypes.UNPROCESSED_EMC, 0.0));
  }

  @NotNull
  public InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide()) {
      if (!ProjectEConfig.server.items.enableTimeWatch.get()) {
        PlayerHelper.sendOverlayMessage(player, PELang.TIME_WATCH_DISABLED.translate());
        return InteractionResult.FAIL;
      }

      stack.update(
          PEDataComponentTypes.TIME_WATCH_MODE,
          TimeWatch.TimeWatchMode.OFF,
          TimeWatch.TimeWatchMode::next);
      PlayerHelper.sendOverlayMessage(
          player, PELang.TIME_WATCH_MODE_SWITCH.translate(this.getTimeName(stack)));
    }

    return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
  }

  @Override
  public boolean changeMode(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    if (!ProjectEConfig.server.items.enableTimeWatch.get()) {
      if (!player.level().isClientSide()) {
        PlayerHelper.sendOverlayMessage(player, PELang.TIME_WATCH_DISABLED.translate());
      }
      return true;
    }

    return super.changeMode(player, stack, hand);
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (entity instanceof Player player
        && hotBarOrOffHand(stack, entity, slot)
        && ProjectEConfig.server.items.enableTimeWatch.get()) {
      TimeWatch.TimeWatchMode timeControl =
          (TimeWatch.TimeWatchMode)
              stack.getOrDefault(PEDataComponentTypes.TIME_WATCH_MODE, TimeWatch.TimeWatchMode.OFF);
      if (timeControl != TimeWatch.TimeWatchMode.OFF
          && (Boolean) level.getGameRules().get(GameRules.ADVANCE_TIME)) {
        long scaledCharge = 4L * (this.getCharge(stack) + 1);
        level
            .dimensionType()
            .defaultClock()
            .ifPresent(
                clock -> {
                  long clockTime = level.clockManager().getTotalTicks(clock);
                  if (timeControl == TimeWatch.TimeWatchMode.REWIND) {
                    level
                        .clockManager()
                        .setTotalTicks(clock, Math.max(clockTime - scaledCharge, 0L));
                  } else {
                    level
                        .clockManager()
                        .setTotalTicks(
                            clock,
                            clockTime > Long.MAX_VALUE - scaledCharge
                                ? Long.MAX_VALUE
                                : clockTime + scaledCharge);
                  }
                });
      }

      if (!level.isClientSide()
          && (Boolean) stack.getOrDefault(PEDataComponentTypes.ACTIVE, false)) {
        int charge = this.getCharge(stack);
        long reqEmc = EMCHelper.removeFractionalEMC(stack, this.getEmcPerTick(charge));
        if (consumeFuel(player, stack, reqEmc, true)) {
          int bonusTicks;
          float mobSlowdown;
          if (charge == 0) {
            bonusTicks = 8;
            mobSlowdown = 0.25F;
          } else if (charge == 1) {
            bonusTicks = 12;
            mobSlowdown = 0.16F;
          } else {
            bonusTicks = 16;
            mobSlowdown = 0.12F;
          }

          AABB effectBounds = player.getBoundingBox().inflate(8.0);
          this.speedUpBlocks(level, bonusTicks, effectBounds);
          this.slowMobs(level, effectBounds, mobSlowdown);
        }
      }
    }
  }

  private void slowMobs(Level level, AABB effectBounds, double mobSlowdown) {
    if (mobSlowdown < 1.0) {
      for (Mob ent : level.getEntitiesOfClass(Mob.class, effectBounds)) {
        ent.setDeltaMovement(ent.getDeltaMovement().multiply(mobSlowdown, 1.0, mobSlowdown));
      }
    }
  }

  private void speedUpBlocks(Level level, int bonusTicks, AABB effectBounds) {
    if (bonusTicks > 0) {
      this.speedUpBlockEntities(level, bonusTicks, effectBounds);
      this.speedUpRandomTicks(level, bonusTicks, effectBounds);
    }
  }

  private void speedUpBlockEntities(Level level, int bonusTicks, AABB effectBounds) {
    if (level instanceof ServerLevel serverLevel) {
      for (BlockEntity blockEntity :
          WorldHelper.getBlockEntitiesWithinAABB(level, effectBounds, VALID_TARGET)) {
        BlockPos pos = blockEntity.getBlockPos();
        long chunkPos = ChunkPos.pack(pos);
        if (serverLevel.shouldTickBlocksAt(pos)
            && serverLevel.isPositionTickingWithEntitiesLoaded(chunkPos)) {
          BlockState state = level.getBlockState(pos);
          this.provideBonusTicks(level, pos, state, blockEntity, bonusTicks);
        }
      }
    }
  }

  private void provideBonusTicks(
      Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, int bonusTicks) {
    if (blockEntity.getType().isValid(state)
        && state.getBlock() instanceof EntityBlock entityBlock) {
      BlockEntityTicker ticker = entityBlock.getTicker(level, state, blockEntity.getType());
      if (ticker != null) {
        for (int i = 0; i < bonusTicks && !blockEntity.isRemoved(); i++) {
          ticker.tick(level, pos, state, blockEntity);
        }
      }
    }
  }

  private void speedUpRandomTicks(Level level, int bonusTicks, AABB effectBounds) {
    if (level instanceof ServerLevel serverLevel) {
      for (BlockPos pos : WorldHelper.getPositionsInBox(effectBounds)) {
        if (WorldHelper.isBlockLoaded(level, pos)) {
          BlockState state = level.getBlockState(pos);
          if (state.isRandomlyTicking() && !state.is(PETags.Blocks.BLACKLIST_TIME_WATCH)) {
            FluidState fluidState = state.getFluidState();
            if ((fluidState.isEmpty() || fluidState.isSource()) && !WorldHelper.isCrop(state)) {
              pos = pos.immutable();

              for (int i = 0; i < bonusTicks; i++) {
                state.randomTick(serverLevel, pos, level.getRandom());
              }
            }
          }
        }
      }
    }
  }

  private ILangEntry getTimeName(ItemStack stack) {
    return ((TimeWatch.TimeWatchMode)
            stack.getOrDefault(PEDataComponentTypes.TIME_WATCH_MODE, TimeWatch.TimeWatchMode.OFF))
        .name;
  }

  public double getEmcPerTick(int charge) {
    return (charge + 2) / 2.0;
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(PELang.TOOLTIP_TIME_WATCH_1.translate());
    tooltip.accept(PELang.TOOLTIP_TIME_WATCH_2.translate(Component.keybind("key.use")));
    tooltip.accept(PELang.TIME_WATCH_MODE.translate(this.getTimeName(stack)));
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    if (!level.isClientSide() && ProjectEConfig.server.items.enableTimeWatch.get()) {
      AABB effectBounds = pedestal.getEffectBounds();
      this.speedUpBlocks(level, ProjectEConfig.server.effects.timePedBonus.get(), effectBounds);
      this.slowMobs(level, effectBounds, ProjectEConfig.server.effects.timePedMobSlowness.get());
    }

    return false;
  }

  @NotNull
  @Override
  public List<Component> getPedestalDescription(float tickRate) {
    List<Component> list = new ArrayList<>();
    if (ProjectEConfig.server.effects.timePedBonus.get() > 0) {
      list.add(
          PELang.PEDESTAL_TIME_WATCH_1.translateColored(
              ChatFormatting.BLUE, ProjectEConfig.server.effects.timePedBonus.get()));
    }

    if (ProjectEConfig.server.effects.timePedMobSlowness.get() < 1.0) {
      list.add(
          PELang.PEDESTAL_TIME_WATCH_2.translateColored(
              ChatFormatting.BLUE,
              String.format("%.3f", ProjectEConfig.server.effects.timePedMobSlowness.get())));
    }

    return list;
  }

  @Override
  public int getNumCharges(@NotNull ItemStack stack) {
    return 2;
  }

  @Override
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

  public enum TimeWatchMode implements StringRepresentable {
    OFF(PELang.TIME_WATCH_OFF),
    FAST_FORWARD(PELang.TIME_WATCH_FAST_FORWARD),
    REWIND(PELang.TIME_WATCH_REWIND);

    public static final Codec<TimeWatch.TimeWatchMode> CODEC =
        StringRepresentable.fromEnum(TimeWatch.TimeWatchMode::values);
    public static final IntFunction<TimeWatch.TimeWatchMode> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, TimeWatch.TimeWatchMode> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);
    private final ILangEntry name;

    TimeWatchMode(ILangEntry name) {
      this.name = name;
    }

    @NotNull
    public String getSerializedName() {
      return this.serializedName;
    }

    public TimeWatch.TimeWatchMode next() {
      return switch (this) {
        case OFF -> FAST_FORWARD;
        case FAST_FORWARD -> REWIND;
        case REWIND -> OFF;
      };
    }
  }
}
