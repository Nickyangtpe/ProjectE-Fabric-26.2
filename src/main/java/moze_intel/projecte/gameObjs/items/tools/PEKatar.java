package moze_intel.projecte.gameObjs.items.tools;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.event.ItemAttributeModifierEvent;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import moze_intel.projecte.gameObjs.items.IItemMode;
import moze_intel.projecte.gameObjs.items.IModeEnum;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.ToolHelper;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class PEKatar extends PETool
    implements IItemMode<PEKatar.KatarMode>, IExtraFunction, IHasConditionalAttributes {
  public PEKatar(IMatterType matterType, int numCharges, Properties props) {
    super(
        matterType,
        PETags.Blocks.MINEABLE_WITH_PE_KATAR,
        numCharges,
        props
            .attributes(createAttributes(matterType, 19.0F, -2.4F))
            .component(PEDataComponentTypes.KATAR_MODE.get(), PEKatar.KatarMode.SLAY_HOSTILE));
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(this.getToolTip(stack));
  }

  @Override
  public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility toolAction) {
    return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(toolAction)
        || ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(toolAction)
        || ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(toolAction)
        || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(toolAction)
        || ToolHelper.DEFAULT_PE_KATAR_ACTIONS.contains(toolAction);
  }

  @NotNull
  public AABB getSweepHitBox(
      @NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target) {
    int charge = this.getCharge(stack);
    return target.getBoundingBox().inflate(charge, charge / 4.0, charge);
  }

  @Override
  protected float getShortCutDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
    float destroySpeed = super.getShortCutDestroySpeed(stack, state);
    return destroySpeed == 1.0F && state.is(BlockTags.SWORD_EFFICIENT) ? 1.5F : destroySpeed;
  }

  @NotNull
  public InteractionResult useOn(UseOnContext context) {
    Player player = context.getPlayer();
    if (player == null) {
      return InteractionResult.PASS;
    }

    Level level = context.getLevel();
    BlockState blockState = level.getBlockState(context.getClickedPos());
    return ToolHelper.performActions(
        context,
        blockState,
        ToolHelper.stripLogsAOE(context, blockState, 0L),
        (ctx, state) -> ToolHelper.scrapeAOE(ctx, state, 0L),
        (ctx, state) -> ToolHelper.waxOffAOE(ctx, state, 0L),
        (ctx, state) -> ToolHelper.tillAOE(ctx, state, 0L),
        (ctx, state) ->
            (InteractionResult)
                (state.is(BlockTags.LOGS)
                    ? ToolHelper.clearTagAOE(
                        ctx.getLevel(),
                        ctx.getPlayer(),
                        ctx.getHand(),
                        ctx.getItemInHand(),
                        0L,
                        BlockTags.LOGS)
                    : InteractionResult.PASS),
        (ctx, state) ->
            (InteractionResult)
                (state.is(BlockTags.LEAVES)
                    ? ToolHelper.clearTagAOE(
                        ctx.getLevel(),
                        ctx.getPlayer(),
                        ctx.getHand(),
                        ctx.getItemInHand(),
                        0L,
                        BlockTags.LEAVES)
                    : InteractionResult.PASS));
  }

  public void hurtEnemy(
      @NotNull ItemStack stack, @NotNull LivingEntity damaged, @NotNull LivingEntity damager) {
    ToolHelper.attackWithCharge(stack, damaged, damager, 1.0F);
  }

  @NotNull
  public InteractionResult use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    return ItemHelper.actionResultFromType(
        ToolHelper.shearEntityAOE(player, hand, 0L), player.getItemInHand(hand));
  }

  @Override
  public boolean doExtraFunction(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    if (player.getAttackStrengthScale(0.0F) == 1.0F) {
      ToolHelper.attackAOE(
          stack,
          player,
          this.getMode(stack) == PEKatar.KatarMode.SLAY_ALL,
          ProjectEConfig.server.difficulty.katarDeathAura.get(),
          0L,
          hand);
      PlayerHelper.resetCooldown(player);
      return true;
    } else {
      return false;
    }
  }

  @NotNull
  public ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
    return ItemUseAnimation.BLOCK;
  }

  public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
    return 72000;
  }

  @Override
  public void adjustAttributes(ItemAttributeModifierEvent event) {
    ToolHelper.applyChargeAttributes(event);
  }

  @NotNull
  public InteractionResult interactLivingEntity(
      @NotNull ItemStack stack,
      @NotNull Player player,
      @NotNull LivingEntity entity,
      @NotNull InteractionHand hand) {
    return ToolHelper.shearEntity(player, stack, entity);
  }

  @Override
  public DataComponentType<PEKatar.KatarMode> getDataComponentType() {
    return PEDataComponentTypes.KATAR_MODE.get();
  }

  public PEKatar.KatarMode getDefaultMode() {
    return PEKatar.KatarMode.SLAY_HOSTILE;
  }

  public enum KatarMode implements IModeEnum<PEKatar.KatarMode> {
    SLAY_HOSTILE(PELang.MODE_KATAR_1),
    SLAY_ALL(PELang.MODE_KATAR_2);

    public static final Codec<PEKatar.KatarMode> CODEC =
        StringRepresentable.fromEnum(PEKatar.KatarMode::values);
    public static final IntFunction<PEKatar.KatarMode> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PEKatar.KatarMode> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final IHasTranslationKey langEntry;
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    KatarMode(IHasTranslationKey langEntry) {
      this.langEntry = langEntry;
    }

    public String getSerializedName() {
      return this.serializedName;
    }

    @Override
    public String getTranslationKey() {
      return this.langEntry.getTranslationKey();
    }

    public PEKatar.KatarMode next(ItemStack stack) {
      return this == SLAY_HOSTILE ? SLAY_ALL : SLAY_HOSTILE;
    }
  }
}
