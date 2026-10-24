package moze_intel.projecte.gameObjs.items.rings;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalRangeItem;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.ICapabilityAware;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class LifeStone extends PEToggleItem implements IPedestalRangeItem, ICapabilityAware {
  public LifeStone(Properties props) {
    super(props.component(PEDataComponentTypes.STORED_EMC, 0L));
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (!level.isClientSide()
        && hotBarOrOffHand(stack, entity, slot)
        && entity instanceof Player player) {
      if ((Boolean) stack.getOrDefault(PEDataComponentTypes.ACTIVE, false)) {
        if (consumeFuel(player, stack, 128L, false)) {
          if (PlayerHelper.checkFeedCooldown(player)) {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                PESoundEvents.HEAL.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
            player.getFoodData().eat(2, 10.0F);
            player.gameEvent(GameEvent.EAT);
            removeEmc(stack, 64L);
          }

          if (PlayerHelper.checkHealCooldown(player)) {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                PESoundEvents.HEAL.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
            player.heal(2.0F);
            removeEmc(stack, 64L);
          }
        } else {
          stack.set(PEDataComponentTypes.ACTIVE, false);
        }
      }
    }
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.life.get() != -1) {
      if (pedestal.getActivityCooldown() == 0) {
        for (ServerPlayer player :
            level.getEntitiesOfClass(ServerPlayer.class, pedestal.getEffectBounds())) {
          if (player.getHealth() < player.getMaxHealth()) {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                PESoundEvents.HEAL.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
            player.heal(1.0F);
          }

          if (player.getFoodData().needsFood()) {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                PESoundEvents.HEAL.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
            player.getFoodData().eat(1, 1.0F);
            player.gameEvent(GameEvent.EAT);
          }
        }

        pedestal.setActivityCooldown(
            level, pos, ProjectEConfig.server.cooldown.pedestal.life.get());
      } else {
        pedestal.decrementActivityCooldown(level, pos);
      }
    }

    return false;
  }

  @NotNull
  @Override
  public List<Component> getPedestalDescription(float tickRate) {
    List<Component> list = new ArrayList<>();
    if (ProjectEConfig.server.cooldown.pedestal.life.get() != -1) {
      list.add(PELang.PEDESTAL_LIFE_STONE_1.translateColored(ChatFormatting.BLUE));
      list.add(
          PELang.PEDESTAL_LIFE_STONE_2.translateColored(
              ChatFormatting.BLUE,
              MathUtils.tickToSecFormatted(
                  ProjectEConfig.server.cooldown.pedestal.life.get(), tickRate)));
    }

    return list;
  }

  @Override
  public void attachCapabilities(RegisterCapabilitiesEvent event) {
    IntegrationHelper.registerCuriosCapability(event, this);
  }
}
