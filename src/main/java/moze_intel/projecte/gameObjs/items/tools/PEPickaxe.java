package moze_intel.projecte.gameObjs.items.tools;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ItemAbility;
import moze_intel.projecte.compat.common.ItemAbilityProvider;
import moze_intel.projecte.compat.common.Tags;
import moze_intel.projecte.compat.common.ToolRuleHelper;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.items.IItemMode;
import moze_intel.projecte.gameObjs.items.IModeEnum;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.ToolHelper;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class PEPickaxe extends Item
    implements IItemCharge, IItemMode<PEPickaxe.PickaxeMode>, IBarHelper, ItemAbilityProvider {
  private final IMatterType matterType;
  private final int numCharges;

  public PEPickaxe(IMatterType matterType, int numCharges, Properties props) {
    super(
        props
            .attributes(PETool.createAttributes(matterType, 4.0F, -2.8F))
            .component(
                DataComponents.TOOL,
                new Tool(
                    List.of(
                        Rule.deniesDrops(
                            ToolRuleHelper.fromTag(matterType.getIncorrectBlocksForDrops())),
                        Rule.minesAndDrops(
                            ToolRuleHelper.fromTag(BlockTags.MINEABLE_WITH_PICKAXE),
                            matterType.getSpeed())),
                    1.0F,
                    1,
                    true))
            .component(PEDataComponentTypes.PICKAXE_MODE.get(), PEPickaxe.PickaxeMode.STANDARD)
            .component(PEDataComponentTypes.CHARGE.get(), 0));
    this.matterType = matterType;
    this.numCharges = numCharges;
  }

  @Override
  public boolean canPerformAction(ItemStack stack, ItemAbility action) {
    return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(action);
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

  public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
    return ToolHelper.canMatterMine(this.matterType, state.getBlock())
        ? 1200000.0F
        : ToolHelper.getDestroySpeed(
            super.getDestroySpeed(stack, state), this.matterType, this.getCharge(stack));
  }

  @Override
  public int getNumCharges(@NotNull ItemStack stack) {
    return this.numCharges;
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

  @NotNull
  public InteractionResult use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    return (InteractionResult)
        (ProjectEConfig.server.items.pickaxeAoeVeinMining.get()
            ? ItemHelper.actionResultFromType(ToolHelper.mineOreVeinsInAOE(player, hand), stack)
            : InteractionResult.PASS);
  }

  @NotNull
  public InteractionResult useOn(UseOnContext context) {
    Player player = context.getPlayer();
    if (player != null && !ProjectEConfig.server.items.pickaxeAoeVeinMining.get()) {
      BlockPos pos = context.getClickedPos();
      return (InteractionResult)
          (context.getLevel().getBlockState(pos).is(Tags.Blocks.ORES)
              ? ToolHelper.tryVeinMine(
                  player, context.getItemInHand(), pos, context.getClickedFace())
              : InteractionResult.PASS);
    } else {
      return InteractionResult.PASS;
    }
  }

  public boolean mineBlock(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockState state,
      @NotNull BlockPos pos,
      @NotNull LivingEntity living) {
    return true;
  }

  @Override
  public DataComponentType<PEPickaxe.PickaxeMode> getDataComponentType() {
    return PEDataComponentTypes.PICKAXE_MODE.get();
  }

  public PEPickaxe.PickaxeMode getDefaultMode() {
    return PEPickaxe.PickaxeMode.STANDARD;
  }

  public static BlockHitResult getPlayerPOVHitResultForMode(Level level, Player player) {
    return getPlayerPOVHitResult(level, player, Fluid.NONE);
  }

  public enum PickaxeMode implements IModeEnum<PEPickaxe.PickaxeMode> {
    STANDARD(PELang.MODE_PICK_1),
    TALLSHOT(PELang.MODE_PICK_2),
    WIDESHOT(PELang.MODE_PICK_3),
    LONGSHOT(PELang.MODE_PICK_4);

    public static final Codec<PEPickaxe.PickaxeMode> CODEC =
        StringRepresentable.fromEnum(PEPickaxe.PickaxeMode::values);
    public static final IntFunction<PEPickaxe.PickaxeMode> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PEPickaxe.PickaxeMode> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final IHasTranslationKey langEntry;
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    PickaxeMode(IHasTranslationKey langEntry) {
      this.langEntry = langEntry;
    }

    public String getSerializedName() {
      return this.serializedName;
    }

    @Override
    public String getTranslationKey() {
      return this.langEntry.getTranslationKey();
    }

    public PEPickaxe.PickaxeMode next(ItemStack stack) {
      return switch (this) {
        case STANDARD -> TALLSHOT;
        case TALLSHOT -> WIDESHOT;
        case WIDESHOT -> LONGSHOT;
        case LONGSHOT -> STANDARD;
      };
    }
  }
}
