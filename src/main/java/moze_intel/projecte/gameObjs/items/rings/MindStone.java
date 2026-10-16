package moze_intel.projecte.gameObjs.items.rings;

import java.util.List;
import java.util.function.Consumer;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class MindStone extends PEToggleItem implements IPedestalItem {
  private static final int TRANSFER_RATE = 50;

  public MindStone(Properties props) {
    super(props.component(PEDataComponentTypes.STORED_EXP, 0));
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (!level.isClientSide()
        && hotBarOrOffHand(stack, entity, slot)
        && entity instanceof Player player
        && (Boolean) stack.getOrDefault(PEDataComponentTypes.ACTIVE, false)
        && this.getXP(player) > 0) {
      int toAdd = Math.min(this.getXP(player), 50);
      this.addStoredXP(stack, toAdd);
      this.removeXP(player, 50);
    }
  }

  @NotNull
  public InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide()
        && !(Boolean) stack.getOrDefault(PEDataComponentTypes.ACTIVE, false)
        && this.getStoredXP(stack) != 0) {
      int toAdd = this.removeStoredXP(stack, 50);
      if (toAdd > 0) {
        this.addXP(player, toAdd);
      }
    }

    return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    int storedXp = (Integer) stack.getOrDefault(PEDataComponentTypes.STORED_EXP, 0);
    tooltip.accept(
        PELang.TOOLTIP_STORED_XP.translateColored(
            ChatFormatting.DARK_GREEN, ChatFormatting.GREEN, String.format("%,d", storedXp)));
  }

  private void removeXP(Player player, int amount) {
    int totalExperience = this.getXP(player) - amount;
    if (totalExperience < 0) {
      player.totalExperience = 0;
      player.experienceLevel = 0;
      player.experienceProgress = 0.0F;
    } else {
      player.totalExperience = totalExperience;
      player.experienceLevel = this.getLvlForXP(totalExperience);
      player.experienceProgress =
          (float) (totalExperience - this.getXPForLvl(player.experienceLevel))
              / player.getXpNeededForNextLevel();
    }
  }

  private void addXP(Player player, int amount) {
    int experiencetotal = this.getXP(player) + amount;
    player.totalExperience = experiencetotal;
    player.experienceLevel = this.getLvlForXP(experiencetotal);
    player.experienceProgress =
        (float) (experiencetotal - this.getXPForLvl(player.experienceLevel))
            / player.getXpNeededForNextLevel();
  }

  private int getXP(Player player) {
    return (int)
        (this.getXPForLvl(player.experienceLevel)
            + player.experienceProgress * player.getXpNeededForNextLevel());
  }

  private int getXPForLvl(int level) {
    if (level < 0) {
      return Integer.MAX_VALUE;
    } else if (level <= 16) {
      return level * level + 6 * level;
    } else {
      return level <= 31
          ? (int) (level * level * 2.5 - 40.5 * level + 360.0)
          : (int) (level * level * 4.5 - 162.5 * level + 2220.0);
    }
  }

  private int getLvlForXP(int totalXP) {
    int result = 0;

    while (this.getXPForLvl(result) <= totalXP) {
      result++;
    }

    return result - 1;
  }

  private int getStoredXP(ItemStack stack) {
    return (Integer) stack.getOrDefault(PEDataComponentTypes.STORED_EXP, 0);
  }

  private void setStoredXP(ItemStack stack, int XP) {
    stack.set(PEDataComponentTypes.STORED_EXP, XP);
  }

  private void addStoredXP(ItemStack stack, int XP) {
    int stored = this.getStoredXP(stack);
    if (stored > Integer.MAX_VALUE - XP) {
      this.setStoredXP(stack, Integer.MAX_VALUE);
    } else {
      this.setStoredXP(stack, stored + XP);
    }
  }

  private int removeStoredXP(ItemStack stack, int XP) {
    int currentXP = this.getStoredXP(stack);
    int result;
    int returnResult;
    if (currentXP < XP) {
      result = 0;
      returnResult = currentXP;
    } else {
      result = currentXP - XP;
      returnResult = XP;
    }

    this.setStoredXP(stack, result);
    return returnResult;
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    boolean sucked = false;
    Vec3 target = Vec3.atCenterOf(pos);

    for (ExperienceOrb orb :
        level.getEntitiesOfClass(ExperienceOrb.class, pedestal.getEffectBounds())) {
      WorldHelper.gravitateEntityTowards(orb, target);
      if (!level.isClientSide() && orb.distanceToSqr(target) < 1.21) {
        this.suckXP(orb, stack);
        sucked = true;
      }
    }

    return sucked;
  }

  private void suckXP(ExperienceOrb orb, ItemStack mindStone) {
    long l = this.getStoredXP(mindStone);
    if (l + orb.getValue() > 2147483647L) {
      orb.setValue((int) (l + orb.getValue() - 2147483647L));
      this.setStoredXP(mindStone, Integer.MAX_VALUE);
    } else {
      this.addStoredXP(mindStone, orb.getValue());
      orb.discard();
    }
  }

  @NotNull
  @Override
  public List<Component> getPedestalDescription(float tickRate) {
    return List.of(PELang.PEDESTAL_MIND_STONE.translateColored(ChatFormatting.BLUE));
  }
}
