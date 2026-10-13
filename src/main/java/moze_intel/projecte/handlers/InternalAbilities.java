package moze_intel.projecte.handlers;

import com.google.common.base.Predicates;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEAttributes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

public class InternalAbilities {
  private static final AttributeModifier WATER_SPEED_BOOST =
      new AttributeModifier(PECore.rl("water_speed_boost"), 0.15, Operation.ADD_VALUE);
  private static final AttributeModifier LAVA_SPEED_BOOST =
      new AttributeModifier(PECore.rl("lava_speed_boost"), 0.15, Operation.ADD_VALUE);

  public static void tick(Player player) {
    Predicate<Player> applyWaterSpeed = Predicates.alwaysFalse();
    Predicate<Player> applyLavaSpeed = Predicates.alwaysFalse();
    InternalAbilities.WalkOnType waterWalkOnType = canWalkOnWater(player);
    InternalAbilities.WalkOnType lavaWalkOnType = canWalkOnLava(player);
    if (waterWalkOnType.canWalk() || lavaWalkOnType.canWalk()) {
      FluidState below = player.level().getFluidState(player.getOnPos());
      boolean water = waterWalkOnType.canWalk() && below.is(FluidTags.WATER);
      boolean lava = lavaWalkOnType.canWalk() && below.is(FluidTags.LAVA);
      if ((water || lava) && player.getInBlockState().isAir()) {
        if (!player.isShiftKeyDown()) {
          player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
          player.resetFallDistance();
          player.setOnGround(true);
        }

        applyWaterSpeed = waterWalkOnType.applySpeed(water);
        applyLavaSpeed = lavaWalkOnType.applySpeed(lava);
      } else if (!player.level().isClientSide()
          && waterWalkOnType.canWalk()
          && player.isInWater()) {
        player.setAirSupply(player.getMaxAirSupply());
      }
    }

    if (!player.level().isClientSide()) {
      updateAttribute(player, Attributes.MOVEMENT_SPEED, WATER_SPEED_BOOST, applyWaterSpeed);
      updateAttribute(player, Attributes.MOVEMENT_SPEED, LAVA_SPEED_BOOST, applyLavaSpeed);
      updateFlight(player);
    }
  }

  private static void updateAttribute(
      Player player,
      Holder<Attribute> attribute,
      AttributeModifier modifier,
      Predicate<Player> applyAttribute) {
    AttributeInstance attributeInstance = player.getAttribute(attribute);
    if (attributeInstance != null) {
      boolean hasModifier = attributeInstance.hasModifier(modifier.id());
      if (applyAttribute.test(player)) {
        if (!hasModifier) {
          attributeInstance.addTransientModifier(modifier);
        }
      } else if (hasModifier) {
        attributeInstance.removeModifier(modifier.id());
      }
    }
  }

  private static void updateFlight(Player player) {
    if (!player.isCreative()) {
      boolean mayFly =
          player.getAttributeValue(PEAttributes.CREATIVE_FLIGHT) > 0.0 || shouldPlayerFly(player);
      if (player.getAbilities().mayfly != mayFly) {
        player.getAbilities().mayfly = mayFly;
        if (!mayFly) {
          player.getAbilities().flying = false;
        }

        player.onUpdateAbilities();
      }
    }
  }

  private static boolean shouldPlayerFly(Player player) {
    BiPredicate<Player, ItemStack> hasPoweredGale =
        (p, stack) ->
            stack.is(PEItems.SWIFTWOLF_RENDING_GALE.get()) && ItemPE.hasEmc(p, stack, 64L, true);
    return PlayerHelper.checkHotbarCurios(player, hasPoweredGale)
        || PlayerHelper.checkMainInventory(player, hasPoweredGale)
        || PlayerHelper.checkHotbar(player, (p, stack) -> stack.is(PEItems.ARCANA_RING.get()));
  }

  private static InternalAbilities.WalkOnType canWalkOnWater(Player player) {
    if (PlayerHelper.checkHotbarCurios(
        player, (p, stack) -> stack.is(PEItems.EVERTIDE_AMULET.get()))) {
      return InternalAbilities.WalkOnType.ABLE_WITH_SPEED;
    }

    ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
    return !helmet.isEmpty() && helmet.is(PEItems.GEM_HELMET.get())
        ? InternalAbilities.WalkOnType.ABLE
        : InternalAbilities.WalkOnType.UNABLE;
  }

  private static InternalAbilities.WalkOnType canWalkOnLava(Player player) {
    if (PlayerHelper.checkHotbarCurios(
        player, (p, stack) -> stack.is(PEItems.VOLCANITE_AMULET.get()))) {
      return InternalAbilities.WalkOnType.ABLE_WITH_SPEED;
    }

    ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
    return !chestplate.isEmpty() && chestplate.is(PEItems.GEM_CHESTPLATE.get())
        ? InternalAbilities.WalkOnType.ABLE
        : InternalAbilities.WalkOnType.UNABLE;
  }

  private enum WalkOnType {
    ABLE,
    ABLE_WITH_SPEED,
    UNABLE;

    public boolean canWalk() {
      return this != UNABLE;
    }

    public Predicate<Player> applySpeed(boolean onType) {
      return onType && this == ABLE_WITH_SPEED ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
  }
}
