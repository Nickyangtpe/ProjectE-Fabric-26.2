package moze_intel.projecte.gameObjs.items;

import java.util.function.IntFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntityLensProjectile;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class HyperkineticLens extends ItemPE
    implements IProjectileShooter, IItemCharge, IBarHelper {
  public HyperkineticLens(Properties props) {
    super(
        props
            .component(PEDataComponentTypes.CHARGE, 0)
            .component(PEDataComponentTypes.STORED_EMC, 0L));
  }

  @NotNull
  public InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide()
        && PlayerHelper.checkCooldown(
            player, stack.getItem(), ProjectEConfig.server.cooldown.player.projectile)) {
      this.shootProjectile(player, stack, hand);
    }

    return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
  }

  @Override
  public boolean shootProjectile(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    HyperkineticLens.ExplosiveLensCharge charge =
        HyperkineticLens.ExplosiveLensCharge.BY_ID.apply(this.getCharge(stack));
    if (!consumeFuel(player, stack, charge.emcCost(), true)) {
      PlayerHelper.sendOverlayMessage(player, PELang.EMC_INSUFFICIENT.translate());
      return false;
    }

    Level level = player.level();
    level.playSound(
        null,
        player.getX(),
        player.getY(),
        player.getZ(),
        PESoundEvents.POWER.get(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
    EntityLensProjectile ent = new EntityLensProjectile(player, charge);
    ent.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
    level.addFreshEntity(ent);
    return true;
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

  public enum ExplosiveLensCharge {
    UNCHARGED(4.0F, 384L),
    SINGLE_CHARGE(8.0F, 768L),
    DOUBLE_CHARGE(12.0F, 1536L),
    MAX_CHARGE(16.0F, 2304L);

    public static final IntFunction<HyperkineticLens.ExplosiveLensCharge> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.CLAMP);
    private final float radius;
    private final long emcCost;

    ExplosiveLensCharge(float radius, long emcCost) {
      this.radius = radius;
      this.emcCost = emcCost;
    }

    public float radius() {
      return this.radius;
    }

    public long emcCost() {
      return this.emcCost;
    }
  }
}
