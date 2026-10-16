package moze_intel.projecte.gameObjs.items;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap.Entry;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.api.world_transmutation.IWorldTransmutationFunction;
import moze_intel.projecte.gameObjs.container.PhilosStoneContainer;
import moze_intel.projecte.gameObjs.entity.EntityMobRandomizer;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.world_transmutation.WorldTransmutationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;

public class PhilosophersStone extends ItemMode<PhilosophersStone.PhilosophersStoneMode>
    implements IProjectileShooter, IExtraFunction, ISelfCraftingRemainder {
  public PhilosophersStone(Properties props) {
    super(
        props.component(
            PEDataComponentTypes.PHILOSOPHERS_STONE_MODE,
            PhilosophersStone.PhilosophersStoneMode.CUBE),
        4);
  }

  public BlockHitResult getHitBlock(Level level, Player player, boolean isSneaking) {
    return getPlayerPOVHitResult(level, player, isSneaking ? Fluid.SOURCE_ONLY : Fluid.NONE);
  }

  @NotNull
  public InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext ctx) {
    Level level = ctx.getLevel();
    Player player = ctx.getPlayer();
    BlockPos pos = ctx.getClickedPos();
    Direction sideHit = ctx.getClickedFace();
    boolean isSneaking = ctx.isSecondaryUseActive();
    if (isSneaking && player != null) {
      BlockHitResult rtr = this.getHitBlock(level, player, true);
      if (rtr.getType() == Type.BLOCK && !rtr.getBlockPos().equals(pos)) {
        pos = rtr.getBlockPos();
        sideHit = rtr.getDirection();
      }
    }

    if (level.isClientSide()) {
      return (InteractionResult)
          (WorldTransmutationManager.INSTANCE.getWorldTransmutation(level.getBlockState(pos), true)
                  == null
              ? InteractionResult.PASS
              : InteractionResult.SUCCESS);
    }

    Object2ReferenceMap<BlockPos, BlockState> toChange =
        getChanges(
            level,
            pos,
            sideHit,
            ctx.getHorizontalDirection(),
            isSneaking,
            this.getMode(stack),
            this.getCharge(stack));
    if (toChange.isEmpty()) {
      return InteractionResult.PASS;
    }

    Iterator<Entry<BlockPos, BlockState>> iterator = Object2ReferenceMaps.fastIterator(toChange);

    while (iterator.hasNext()) {
      Entry<BlockPos, BlockState> entry = iterator.next();
      BlockPos currentPos = (BlockPos) entry.getKey();
      BlockState targetState = (BlockState) entry.getValue();
      if (player == null) {
        if (targetState.getBlock() instanceof SignBlock
            && level.getBlockEntity(currentPos) instanceof SignBlockEntity sign) {
          level.setBlockAndUpdate(currentPos, targetState);
          WorldHelper.copySignData(level, currentPos, sign);
        } else {
          level.setBlockAndUpdate(currentPos, targetState);
        }
      } else {
        PlayerHelper.checkedReplaceBlock((ServerPlayer) player, level, currentPos, targetState);
      }

      if (level.getRandom().nextInt(8) == 0) {
        ((ServerLevel) level)
            .sendParticles(
                ParticleTypes.LARGE_SMOKE,
                currentPos.getX(),
                currentPos.getY() + 1,
                currentPos.getZ(),
                2,
                0.0,
                0.0,
                0.0,
                0.0);
      }
    }

    BlockPos soundPos = player == null ? pos : player.blockPosition();
    level.playSound(
        null,
        soundPos.getX(),
        soundPos.getY(),
        soundPos.getZ(),
        PESoundEvents.TRANSMUTE.get(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
    return InteractionResult.SUCCESS;
  }

  @Override
  public boolean shootProjectile(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    Level level = player.level();
    level.playSound(
        null,
        player.getX(),
        player.getY(),
        player.getZ(),
        PESoundEvents.TRANSMUTE.get(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
    EntityMobRandomizer ent = new EntityMobRandomizer(player, level);
    ent.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
    level.addFreshEntity(ent);
    return true;
  }

  @Override
  public boolean doExtraFunction(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    if (!player.level().isClientSide()) {
      player.openMenu(new PhilosophersStone.ContainerProvider(stack));
    }

    return true;
  }

  @Override
  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(
        PELang.TOOLTIP_PHILOSTONE.translate(ClientKeyHelper.getKeyName(PEKeybind.EXTRA_FUNCTION)));
  }

  public static Object2ReferenceMap<BlockPos, BlockState> getChanges(
      Level level,
      BlockPos pos,
      Direction sideHit,
      Direction horizontalDirection,
      boolean isSneaking,
      PhilosophersStone.PhilosophersStoneMode mode,
      int charge) {
    IWorldTransmutationFunction transmutation =
        WorldTransmutationManager.INSTANCE.getWorldTransmutation(level.getBlockState(pos));
    if (transmutation == null) {
      return Object2ReferenceMaps.emptyMap();
    }

    Iterable<BlockPos> targets =
        switch (mode) {
          case CUBE -> WorldHelper.positionsAround(pos, charge);
          case PANEL -> {
            switch (sideHit.getAxis()) {
              case X:
                yield WorldHelper.positionsAround(pos, 0, charge, charge);
              case Y:
                yield WorldHelper.horizontalPositionsAround(pos, charge);
              case Z:
                yield WorldHelper.positionsAround(pos, charge, charge, 0);
              default:
                throw new MatchException(null, null);
            }
          }
          case LINE -> {
            switch (horizontalDirection.getAxis()) {
              case X:
                yield WorldHelper.positionsAround(pos, charge, 0, 0);
              case Y:
                yield null;
              case Z:
                yield WorldHelper.positionsAround(pos, 0, 0, charge);
              default:
                throw new MatchException(null, null);
            }
          }
        };
    if (targets == null) {
      return Object2ReferenceMaps.emptyMap();
    }

    Object2ReferenceMap<BlockPos, BlockState> changes = new Object2ReferenceOpenHashMap();

    for (BlockPos currentPos : targets) {
      if (WorldHelper.isBlockLoaded(level, currentPos)) {
        BlockState actualResult = transmutation.result(level.getBlockState(currentPos), isSneaking);
        if (actualResult != null) {
          changes.put(currentPos.immutable(), actualResult);
        }
      }
    }

    return changes;
  }

  @Override
  public DataComponentType<PhilosophersStone.PhilosophersStoneMode> getDataComponentType() {
    return PEDataComponentTypes.PHILOSOPHERS_STONE_MODE.get();
  }

  public PhilosophersStone.PhilosophersStoneMode getDefaultMode() {
    return PhilosophersStone.PhilosophersStoneMode.CUBE;
  }

  private record ContainerProvider(ItemStack stack) implements MenuProvider {
    @NotNull
    public AbstractContainerMenu createMenu(
        int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
      return new PhilosStoneContainer(
          windowId,
          playerInventory,
          ContainerLevelAccess.create(player.level(), player.blockPosition()));
    }

    @NotNull
    public Component getDisplayName() {
      return this.stack.getHoverName();
    }
  }

  public enum PhilosophersStoneMode implements IModeEnum<PhilosophersStone.PhilosophersStoneMode> {
    CUBE(PELang.MODE_PHILOSOPHER_1),
    PANEL(PELang.MODE_PHILOSOPHER_2),
    LINE(PELang.MODE_PHILOSOPHER_3);

    public static final Codec<PhilosophersStone.PhilosophersStoneMode> CODEC =
        StringRepresentable.fromEnum(PhilosophersStone.PhilosophersStoneMode::values);
    public static final IntFunction<PhilosophersStone.PhilosophersStoneMode> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PhilosophersStone.PhilosophersStoneMode> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final IHasTranslationKey langEntry;
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    PhilosophersStoneMode(IHasTranslationKey langEntry) {
      this.langEntry = langEntry;
    }

    @NotNull
    public String getSerializedName() {
      return this.serializedName;
    }

    @Override
    public String getTranslationKey() {
      return this.langEntry.getTranslationKey();
    }

    public PhilosophersStone.PhilosophersStoneMode next(ItemStack stack) {
      return switch (this) {
        case CUBE -> PANEL;
        case PANEL -> LINE;
        case LINE -> CUBE;
      };
    }
  }
}
