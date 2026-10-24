package moze_intel.projecte.gameObjs.items.rings;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalRangeItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.util.FakePlayerFactory;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntityHomingArrow;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class ArchangelSmite extends PEToggleItem implements IPedestalRangeItem {
  public ArchangelSmite(Properties props) {
    super(props.component(PEDataComponentTypes.STORED_EMC, 0L));
    AttackBlockCallback.EVENT.register(
        (AttackBlockCallback)
            (player, level, hand, pos, direction) -> {
              ItemStack held = player.getItemInHand(hand);
              if (!level.isClientSide() && held.is(this)) {
                fireVolley(held, player);
              }

              return InteractionResult.PASS;
            });
    AttackEntityCallback.EVENT.register(
        (AttackEntityCallback)
            (player, level, hand, entity, hitResult) -> {
              if (!level.isClientSide() && player.getItemInHand(hand).is(this)) {
                fireVolley(player.getItemInHand(hand), player);
              }

              return InteractionResult.PASS;
            });
  }

  public static void fireVolley(ItemStack stack, Player player) {
    for (int i = 0; i < 10; i++) {
      fireArrow(stack, player.level(), player, 4.0F);
    }
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (!level.isClientSide() && this.getMode(stack) && entity instanceof LivingEntity living) {
      fireArrow(stack, level, living, 1.0F);
    }
  }

  @NotNull
  public InteractionResult use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    if (!level.isClientSide()) {
      fireArrow(player.getItemInHand(hand), level, player, 1.0F);
    }

    return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
  }

  private static void fireArrow(
      ItemStack ring, Level level, LivingEntity shooter, float inaccuracy) {
    EntityHomingArrow arrow = new EntityHomingArrow(level, shooter, 2.0F);
    if (!(shooter instanceof Player player
        && !consumeFuel(player, ring, IEMCProxy.INSTANCE.getValue(Items.ARROW), true))) {
      arrow.shootFromRotation(
          shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 3.0F, inaccuracy);
      level.playSound(
          null,
          shooter.getX(),
          shooter.getY(),
          shooter.getZ(),
          SoundEvents.ARROW_SHOOT,
          SoundSource.PLAYERS,
          1.0F,
          1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F));
      level.addFreshEntity(arrow);
    }
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.archangel.get() != -1) {
      if (pedestal.getActivityCooldown() == 0) {
        if (!level.getEntitiesOfClass(Mob.class, pedestal.getEffectBounds()).isEmpty()) {
          double centeredX = pos.getX() + 0.5;
          double centeredY = pos.getY() + 0.5;
          double centeredZ = pos.getZ() + 0.5;

          for (int i = 0; i < 3; i++) {
            EntityHomingArrow arrow =
                new EntityHomingArrow(
                    level,
                    FakePlayerFactory.get((ServerLevel) level, PECore.FAKEPLAYER_GAMEPROFILE),
                    2.0F);
            // The pedestal is an autonomous device, not a player action. Keeping the synthetic
            // ServerPlayer as owner makes vanilla award the player's "Take Aim" advancement.
            arrow.setOwner(null);
            arrow.setPosRaw(centeredX, centeredY + 2.0, centeredZ);
            arrow.setDeltaMovement(0.0, 1.0, 0.0);
            arrow.playSound(
                SoundEvents.ARROW_SHOOT,
                1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
            level.addFreshEntity(arrow);
          }
        }

        pedestal.setActivityCooldown(
            level, pos, ProjectEConfig.server.cooldown.pedestal.archangel.get());
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
    if (ProjectEConfig.server.cooldown.pedestal.archangel.get() != -1) {
      list.add(PELang.PEDESTAL_ARCHANGEL_1.translateColored(ChatFormatting.BLUE));
      list.add(
          PELang.PEDESTAL_ARCHANGEL_2.translateColored(
              ChatFormatting.BLUE,
              MathUtils.tickToSecFormatted(
                  ProjectEConfig.server.cooldown.pedestal.archangel.get(), tickRate)));
    }

    return list;
  }
}
