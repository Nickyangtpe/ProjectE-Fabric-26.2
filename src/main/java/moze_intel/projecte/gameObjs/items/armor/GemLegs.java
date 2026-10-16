package moze_intel.projecte.gameObjs.items.armor;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.function.Consumer;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.entity.living.LivingEvent;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class GemLegs extends GemArmorBase {
  private static final Vec3 DOWNWARD_MOVEMENT = new Vec3(0.0, -0.32F, 0.0);
  private final Int2LongMap lastJumpTracker = new Int2LongOpenHashMap();

  public GemLegs(Properties props) {
    super(ArmorType.LEGGINGS, props);
    NeoForge.EVENT_BUS.addListener(this::onJump);
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(PELang.GEM_LORE_LEGS.translate());
  }

  private void onJump(LivingEvent.LivingJumpEvent evt) {
    if (evt.getEntity() instanceof Player player && player.level().isClientSide()) {
      this.lastJumpTracker.put(player.getId(), player.level().getGameTime());
    }
  }

  private boolean jumpedRecently(Player player) {
    return this.lastJumpTracker.containsKey(player.getId())
        && player.level().getGameTime() - this.lastJumpTracker.get(player.getId()) < 5L;
  }

  public void applyClientJumpAssist(Player player) {
    if (player.isSecondaryUseActive()
        && !player.onGround()
        && player.getDeltaMovement().y() > -8.0
        && !this.jumpedRecently(player)) {
      player.addDeltaMovement(DOWNWARD_MOVEMENT);
    }
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull ServerLevel level,
      @NotNull Entity entity,
      @NotNull EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (isArmorSlot(slot) && entity instanceof Player player) {
      if (player.isSecondaryUseActive() && player.getDeltaMovement().y() < -0.08) {
        for (Entity e :
            level.getEntities(
                player,
                player.getBoundingBox().move(player.getDeltaMovement()).inflate(2.0),
                ent -> ent.isAlive() && ent.isPickable() && ent instanceof LivingEntity)) {
          e.hurt(
              level.damageSources().playerAttack(player),
              (float) (-player.getDeltaMovement().y()) * 6.0F);
        }
      }

      if (player.isSecondaryUseActive()) {
        WorldHelper.repelEntitiesSWRG(level, player.getBoundingBox().inflate(3.5), player);
      }
    }
  }
}
