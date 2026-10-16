package moze_intel.projecte.gameObjs.blocks;

import java.util.List;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.EnumRelayTier;
import moze_intel.projecte.gameObjs.block_entities.RelayMK1BlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Relay extends BlockDirection implements PEEntityBlock<RelayMK1BlockEntity> {
  private final EnumRelayTier tier;

  public Relay(EnumRelayTier tier, Properties props) {
    super(props);
    this.tier = tier;
  }

  public EnumRelayTier getTier() {
    return this.tier;
  }

  @Deprecated
  @NotNull
  protected InteractionResult useWithoutItem(
      @NotNull BlockState state,
      Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull BlockHitResult rtr) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    RelayMK1BlockEntity relay =
        WorldHelper.getBlockEntity(RelayMK1BlockEntity.class, level, pos, true);
    if (relay != null) {
      ContainerTypeDeferredRegister.openMenu(player, relay, pos);
    }

    return InteractionResult.CONSUME;
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull List<Component> tooltip,
      @NotNull TooltipFlag flags) {
    if (ProjectEConfig.client.statToolTips.get()) {
      tooltip.add(
          PELang.EMC_MAX_OUTPUT_RATE.translateColored(
              ChatFormatting.DARK_PURPLE,
              ChatFormatting.BLUE,
              EMCHelper.formatEmc(this.tier.getChargeRate())));
      tooltip.add(
          PELang.EMC_MAX_STORAGE.translateColored(
              ChatFormatting.DARK_PURPLE,
              ChatFormatting.BLUE,
              EMCHelper.formatEmc(this.tier.getStorage())));
    }
  }

  @Nullable
  @Override
  public BlockEntityTypeRegistryObject<? extends RelayMK1BlockEntity> getType() {
    return switch (this.tier) {
      case MK1 -> PEBlockEntityTypes.RELAY;
      case MK2 -> PEBlockEntityTypes.RELAY_MK2;
      case MK3 -> PEBlockEntityTypes.RELAY_MK3;
    };
  }

  @Deprecated
  public boolean triggerEvent(
      @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param) {
    super.triggerEvent(state, level, pos, id, param);
    return this.triggerBlockEntityEvent(state, level, pos, id, param);
  }

  @Deprecated
  public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
    return true;
  }

  @Deprecated
  public int getAnalogOutputSignal(
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Direction direction) {
    RelayMK1BlockEntity relay =
        WorldHelper.getBlockEntity(RelayMK1BlockEntity.class, level, pos, true);
    return relay == null
        ? 0
        : MathUtils.scaleToRedstone(relay.getStoredEmc(), relay.getMaximumEmc());
  }
}
