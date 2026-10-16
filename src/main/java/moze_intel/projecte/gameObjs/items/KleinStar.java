package moze_intel.projecte.gameObjs.items;

import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class KleinStar extends ItemPE implements IItemEmcHolder, IBarHelper, ICapabilityAware {
  public final KleinStar.KleinTier tier;

  public KleinStar(Properties props, KleinStar.KleinTier tier) {
    super(props.component(PEDataComponentTypes.STORED_EMC, 0L));
    this.tier = tier;
  }

  public boolean isBarVisible(@NotNull ItemStack stack) {
    return this.getStoredEmc(stack) > 0L;
  }

  @Override
  public float getWidthForBar(ItemStack stack) {
    long starEmc = this.getStoredEmc(stack);
    return starEmc == 0L ? 1.0F : (float) (1.0 - (double) starEmc / this.tier.maxEmc);
  }

  public int getBarWidth(@NotNull ItemStack stack) {
    return this.getScaledBarWidth(stack);
  }

  public int getBarColor(@NotNull ItemStack stack) {
    return this.getColorForBar(stack);
  }

  @NotNull
  public InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide() && !FMLEnvironment.production && player.isCreative()) {
      stack.set(PEDataComponentTypes.STORED_EMC, this.getMaximumEmc(stack));
      return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    } else {
      return InteractionResult.PASS;
    }
  }

  @Override
  public long insertEmc(@NotNull ItemStack stack, long toInsert, IEmcStorage.EmcAction action) {
    if (toInsert < 0L) {
      return this.extractEmc(stack, -toInsert, action);
    }

    long maxEmc = this.getMaximumEmc(stack);
    long storedEmc = this.getStoredEmc(stack);
    if (storedEmc >= maxEmc) {
      return 0L;
    }

    long toAdd = Math.min(maxEmc - storedEmc, toInsert);
    if (action.execute()) {
      stack.set(PEDataComponentTypes.STORED_EMC, storedEmc + toAdd);
    }

    return toAdd;
  }

  @Override
  public long extractEmc(@NotNull ItemStack stack, long toExtract, IEmcStorage.EmcAction action) {
    if (toExtract < 0L) {
      return this.insertEmc(stack, -toExtract, action);
    }

    long storedEmc = this.getStoredEmc(stack);
    long toRemove = Math.min(storedEmc, toExtract);
    if (action.execute()) {
      stack.set(PEDataComponentTypes.STORED_EMC, storedEmc - toRemove);
    }

    return toRemove;
  }

  @Override
  public @Range(from = 0L, to = Long.MAX_VALUE) long getStoredEmc(@NotNull ItemStack stack) {
    return (Long) stack.getOrDefault(PEDataComponentTypes.STORED_EMC, 0L);
  }

  @Override
  public @Range(from = 1L, to = Long.MAX_VALUE) long getMaximumEmc(@NotNull ItemStack stack) {
    return this.tier.maxEmc;
  }

  @Override
  public void attachCapabilities(RegisterCapabilitiesEvent event) {
    IntegrationHelper.registerCuriosCapability(event, this);
  }

  public enum KleinTier {
    EIN("ein", 50000L),
    ZWEI("zwei", 200000L),
    DREI("drei", 800000L),
    VIER("vier", 3200000L),
    SPHERE("sphere", 12800000L),
    OMEGA("omega", 51200000L);

    public final String name;
    public final long maxEmc;

    KleinTier(String name, long maxEmc) {
      this.name = name;
      this.maxEmc = maxEmc;
    }
  }
}
