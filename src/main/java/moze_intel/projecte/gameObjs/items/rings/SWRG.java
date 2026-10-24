package moze_intel.projecte.gameObjs.items.rings;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalRangeItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntitySWRGProjectile;
import moze_intel.projecte.gameObjs.items.ICapabilityAware;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.LightningHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SWRG extends ItemPE
    implements IPedestalRangeItem, IProjectileShooter, ICapabilityAware {
  public SWRG(Properties props) {
    super(
        props
            .component(PEDataComponentTypes.SWRG_MODE, SWRG.SWRGMode.OFF)
            .component(PEDataComponentTypes.STORED_EMC, 0L)
            .component(PEDataComponentTypes.UNPROCESSED_EMC, 0.0));
  }

  private void tick(ItemStack stack, Player player) {
    SWRG.SWRGMode mode = this.getMode(stack);
    if (mode.hasShield()) {
      WorldHelper.repelEntitiesSWRG(player.level(), player.getBoundingBox().inflate(5.0), player);
    }

    if (!player.level().isClientSide()) {
      if (!hasEmc(player, stack, 64L, true)) {
        this.changeMode(player, stack, mode, SWRG.SWRGMode.OFF);
      } else {
        if (player.getAbilities().flying) {
          if (!mode.hasFlight()) {
            mode = this.changeMode(player, stack, mode, mode.toggleFlight());
          }
        } else if (mode.hasFlight()) {
          mode = this.changeMode(player, stack, mode, mode.toggleFlight());
        }

        float toRemove = mode.hasShield() ? 0.32F : 0.0F;
        if (mode.hasFlight() && player.getAbilities().flying) {
          toRemove += 0.32F;
        }

        removeEmc(stack, (float) toRemove);
      }
    }
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (entity instanceof Player player) {
      this.tick(stack, player);
    }
  }

  private SWRG.SWRGMode getMode(ItemStack stack) {
    return (SWRG.SWRGMode) stack.getOrDefault(PEDataComponentTypes.SWRG_MODE, SWRG.SWRGMode.OFF);
  }

  @NotNull
  public InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide()) {
      SWRG.SWRGMode oldMode = this.getMode(stack);
      this.changeMode(player, stack, oldMode, oldMode.next());
    }

    return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
  }

  private SWRG.SWRGMode changeMode(
      Player player, ItemStack stack, SWRG.SWRGMode oldMode, SWRG.SWRGMode mode) {
    if (mode == oldMode) {
      return mode;
    }

    stack.set(PEDataComponentTypes.SWRG_MODE, mode);
    if (player == null) {
      return mode;
    }

    if (mode == SWRG.SWRGMode.OFF || oldMode == SWRG.SWRGMode.SHIELDED_FLIGHT) {
      player
          .level()
          .playSound(
              null,
              player.getX(),
              player.getY(),
              player.getZ(),
              PESoundEvents.UNCHARGE.get(),
              SoundSource.PLAYERS,
              0.8F,
              1.0F);
    } else if (oldMode == SWRG.SWRGMode.OFF || mode == SWRG.SWRGMode.SHIELDED_FLIGHT) {
      player
          .level()
          .playSound(
              null,
              player.getX(),
              player.getY(),
              player.getZ(),
              PESoundEvents.HEAL.get(),
              SoundSource.PLAYERS,
              0.8F,
              1.0F);
    }

    return mode;
  }

  public boolean isBarVisible(@NotNull ItemStack stack) {
    return false;
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.swrg.get() != -1) {
      if (pedestal.getActivityCooldown() <= 0) {
        for (Mob living :
            level.getEntitiesOfClass(
                Mob.class,
                pedestal.getEffectBounds(),
                ent ->
                    !ent.isSpectator()
                        && !(ent instanceof TamableAnimal tamableAnimal
                            && tamableAnimal.isTame()))) {
          LightningBolt lightning = LightningHelper.create(level);
          if (lightning != null) {
            lightning.setPos(living.position());
            level.addFreshEntity(lightning);
          }
        }

        pedestal.setActivityCooldown(
            level, pos, ProjectEConfig.server.cooldown.pedestal.swrg.get());
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
    if (ProjectEConfig.server.cooldown.pedestal.swrg.get() != -1) {
      list.add(PELang.PEDESTAL_SWRG_1.translateColored(ChatFormatting.BLUE));
      list.add(
          PELang.PEDESTAL_SWRG_2.translateColored(
              ChatFormatting.BLUE,
              MathUtils.tickToSecFormatted(
                  ProjectEConfig.server.cooldown.pedestal.swrg.get(), tickRate)));
    }

    return list;
  }

  @Override
  public boolean shootProjectile(
      @NotNull Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {
    EntitySWRGProjectile projectile = new EntitySWRGProjectile(player, false, player.level());
    projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
    player.level().addFreshEntity(projectile);
    return true;
  }

  @Override
  public void attachCapabilities(RegisterCapabilitiesEvent event) {
    IntegrationHelper.registerCuriosCapability(event, this);
  }

  public enum SWRGMode implements StringRepresentable {
    OFF,
    FLIGHT,
    SHIELD,
    SHIELDED_FLIGHT;

    public static final Codec<SWRG.SWRGMode> CODEC =
        StringRepresentable.fromEnum(SWRG.SWRGMode::values);
    public static final IntFunction<SWRG.SWRGMode> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, SWRG.SWRGMode> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    @NotNull
    public String getSerializedName() {
      return this.serializedName;
    }

    public boolean hasFlight() {
      return this == FLIGHT || this == SHIELDED_FLIGHT;
    }

    public boolean hasShield() {
      return this == SHIELD || this == SHIELDED_FLIGHT;
    }

    public SWRG.SWRGMode toggleFlight() {
      return switch (this) {
        case OFF -> FLIGHT;
        case FLIGHT -> OFF;
        case SHIELD -> SHIELDED_FLIGHT;
        case SHIELDED_FLIGHT -> SHIELD;
      };
    }

    public SWRG.SWRGMode next() {
      return switch (this) {
        case OFF -> SHIELD;
        case FLIGHT -> SHIELDED_FLIGHT;
        case SHIELD -> OFF;
        case SHIELDED_FLIGHT -> FLIGHT;
      };
    }
  }
}
