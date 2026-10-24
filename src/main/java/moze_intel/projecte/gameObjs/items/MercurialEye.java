package moze_intel.projecte.gameObjs.items;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntFunction;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.compat.items.ComponentItemHandler;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.container.MercurialEyeContainer;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.mixin.BlockPlaceContextAccessor;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ExperienceDropTarget;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MercurialEye extends ItemMode<MercurialEye.MercurialEyeMode>
    implements IExtraFunction, ICapabilityAware {
  public MercurialEye(Properties props) {
    super(
        props
            .component(
                PEDataComponentTypes.MERCURIAL_EYE_MODE, MercurialEye.MercurialEyeMode.CREATION)
            .component(PEDataComponentTypes.EYE_INVENTORY, ItemContainerContents.EMPTY),
        4);
  }

  @Override
  public boolean doExtraFunction(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    int selected = player.getInventory().getSelectedSlot();
    MenuProvider provider =
        new SimpleMenuProvider(
            (id, inv, pl) -> new MercurialEyeContainer(id, inv, hand, selected),
            stack.getHoverName());
    ContainerTypeDeferredRegister.openMenu(
        player,
        provider,
        b -> {
          b.writeEnum(hand);
          b.writeByte(selected);
        });
    return true;
  }

  @NotNull
  public InteractionResult useOn(UseOnContext ctx) {
    ItemStack stack = ctx.getItemInHand();
    Level level = ctx.getLevel();
    return (InteractionResult)
        (level.isClientSide()
            ? InteractionResult.SUCCESS
            : this.formBlocks(
                stack,
                ctx.getPlayer(),
                ctx.getHand(),
                level,
                ctx.getClickedPos(),
                ctx.getClickedFace()));
  }

  @NotNull
  public InteractionResult use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (this.getMode(stack) == MercurialEye.MercurialEyeMode.CREATION) {
      if (level.isClientSide()) {
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      }

      Vec3 targVec = PlayerHelper.getLookTarget(player, 2.0);
      return ItemHelper.actionResultFromType(
          this.formBlocks(stack, player, hand, level, BlockPos.containing(targVec), null), stack);
    } else {
      return InteractionResult.PASS;
    }
  }

  private void playFailureSound(Player player) {
    player
        .level()
        .playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            PESoundEvents.UNCHARGE.get(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F);
  }

  private void showFailure(Player player, PELang message) {
    this.playFailureSound(player);
    PlayerHelper.sendOverlayMessage(player, message.translate());
  }

  private InteractionResult formBlocks(
      ItemStack eye,
      Player player,
      InteractionHand hand,
      Level level,
      BlockPos startingPos,
      @Nullable Direction facing) {
    try (ExperienceDropTarget.Scope ignored =
        ExperienceDropTarget.at(level, Vec3.atLowerCornerOf(startingPos))) {
      return formBlocksAndCollectLoot(eye, player, hand, level, startingPos, facing);
    }
  }

  private InteractionResult formBlocksAndCollectLoot(
      ItemStack eye,
      Player player,
      InteractionHand hand,
      Level level,
      BlockPos startingPos,
      @Nullable Direction facing) {
    IItemHandler inventory = Capabilities.ItemHandler.ITEM.find(eye, null);
    if (inventory == null) {
      return InteractionResult.FAIL;
    }

    ItemStack klein = inventory.getStackInSlot(0);
    if (PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(klein, null) == null) {
      this.showFailure(player, PELang.MERCURIAL_EYE_NO_KLEIN_STAR);
      return InteractionResult.FAIL;
    }

    BlockState startingState = level.getBlockState(startingPos);
    long startingBlockEmc = IEMCProxy.INSTANCE.getValue(startingState.getBlock());
    ItemStack target = inventory.getStackInSlot(1);
    MercurialEye.MercurialEyeMode mode = this.getMode(eye);
    BlockHitResult hitResult;
    if (facing == null) {
      hitResult = new BlockHitResult(Vec3.atCenterOf(startingPos), Direction.UP, startingPos, true);
    } else {
      hitResult =
          new BlockHitResult(
              Vec3.atCenterOf(startingPos.relative(facing)), facing, startingPos, false);
    }

    BlockState newState;
    long newBlockEmc;
    BlockPlaceContext context;
    if (!target.isEmpty()) {
      context = new BlockPlaceContext(level, player, hand, target.copy(), hitResult);
      newState = ItemHelper.stackToState(target, context);
      newBlockEmc = IEMCProxy.INSTANCE.getValue(target);
      if (newBlockEmc == 0L) {
        this.showFailure(player, PELang.MERCURIAL_EYE_INVALID_TARGET);
        return InteractionResult.FAIL;
      }
    } else {
      if (!mode.isExtension()) {
        this.showFailure(player, PELang.MERCURIAL_EYE_NO_TARGET);
        return InteractionResult.FAIL;
      } else if (startingBlockEmc == 0L) {
        this.showFailure(player, PELang.MERCURIAL_EYE_INVALID_TARGET);
        return InteractionResult.FAIL;
      }

      newState = startingState;
      newBlockEmc = startingBlockEmc;
      context =
          new BlockPlaceContext(level, player, hand, new ItemStack(newState.getBlock()), hitResult);
    }

    if (newState != null && !newState.isAir()) {
      ActionFeedback feedback = new ActionFeedback();
      NonNullList<ItemStack> drops = NonNullList.create();
      int charge = this.getCharge(eye);
      int hitTargets = 0;
      if (mode == MercurialEye.MercurialEyeMode.CREATION) {
        if (facing != null
            && (!context.replacingClickedOnBlock()
                || context.isSecondaryUseActive() && !startingState.isAir())) {
          BlockPos offsetPos = startingPos.relative(facing);
          BlockState offsetState = level.getBlockState(offsetPos);
          if (!offsetState.canBeReplaced(context)) {
            return InteractionResult.FAIL;
          }

          long offsetBlockEmc = IEMCProxy.INSTANCE.getValue(offsetState.getBlock());
          if (this.doBlockPlace(
              player,
              level,
              offsetState,
              offsetPos,
              newState,
              eye,
              offsetBlockEmc,
              newBlockEmc,
              drops,
              context,
              feedback)) {
            hitTargets++;
          }
        } else if (this.doBlockPlace(
            player,
            level,
            startingState,
            startingPos,
            newState,
            eye,
            startingBlockEmc,
            newBlockEmc,
            drops,
            context,
            feedback)) {
          hitTargets++;
        }
      } else if (mode == MercurialEye.MercurialEyeMode.PILLAR) {
        AABB bounds = this.getBounds(startingPos, facing, 1, 3 * charge + 2);
        hitTargets +=
            this.fillGaps(
                eye,
                player,
                level,
                context,
                startingState,
                newState,
                newBlockEmc,
                bounds,
                drops,
                feedback);
      } else if (mode == MercurialEye.MercurialEyeMode.EXTENSION_CLASSIC) {
        AABB bounds = this.getBounds(startingPos, facing, charge, 0);
        hitTargets +=
            this.fillGaps(
                eye,
                player,
                level,
                context,
                startingState,
                newState,
                newBlockEmc,
                bounds,
                drops,
                feedback);
      } else if (mode == MercurialEye.MercurialEyeMode.TRANSMUTATION_CLASSIC) {
        AABB bounds = this.getBounds(startingPos, facing, charge, 0);

        for (BlockPos pos : WorldHelper.getPositionsInBox(bounds)) {
          BlockState placedState = level.getBlockState(pos);
          if (placedState == startingState
              && this.doBlockPlace(
                  player,
                  level,
                  placedState,
                  pos.immutable(),
                  newState,
                  eye,
                  startingBlockEmc,
                  newBlockEmc,
                  drops,
                  context,
                  feedback)) {
            hitTargets++;
          }
        }
      } else {
        if (startingState.isAir() || facing == null) {
          return InteractionResult.FAIL;
        }

        LinkedList<BlockPos> possibleBlocks = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        possibleBlocks.add(startingPos);
        visited.add(startingPos);
        int side = 2 * charge + 1;
        int size = side * side;
        int totalTries = size * 4;

        for (int attemptedTargets = 0;
            attemptedTargets < totalTries && !possibleBlocks.isEmpty();
            attemptedTargets++) {
          BlockPos pos = possibleBlocks.poll();
          BlockState checkState = level.getBlockState(pos);
          if (startingState == checkState) {
            BlockPos offsetPos = pos.relative(facing);
            BlockState offsetState = level.getBlockState(offsetPos);
            if (!offsetState.isFaceSturdy(level, offsetPos, facing)) {
              boolean hit = false;
              if (mode == MercurialEye.MercurialEyeMode.EXTENSION) {
                VoxelShape cbBox = startingState.getCollisionShape(level, offsetPos);
                if (level.isUnobstructed(null, cbBox)) {
                  long offsetBlockEmc = IEMCProxy.INSTANCE.getValue(offsetState.getBlock());
                  hit =
                      this.doBlockPlace(
                          player,
                          level,
                          offsetState,
                          offsetPos,
                          newState,
                          eye,
                          offsetBlockEmc,
                          newBlockEmc,
                          drops,
                          context,
                          feedback);
                }
              } else if (mode == MercurialEye.MercurialEyeMode.TRANSMUTATION) {
                hit =
                    this.doBlockPlace(
                        player,
                        level,
                        checkState,
                        pos,
                        newState,
                        eye,
                        startingBlockEmc,
                        newBlockEmc,
                        drops,
                        context,
                        feedback);
              }

              if (hit) {
                if (++hitTargets >= size) {
                  break;
                }

                for (Direction e : Constants.DIRECTIONS) {
                  if (facing.getAxis() != e.getAxis()) {
                    BlockPos offset = pos.relative(e);
                    if (visited.add(offset)) {
                      possibleBlocks.offer(offset);
                    }

                    BlockPos offsetOpposite = pos.relative(e.getOpposite());
                    if (visited.add(offsetOpposite)) {
                      possibleBlocks.offer(offsetOpposite);
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (hitTargets > 0) {
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            PESoundEvents.POWER.get(),
            SoundSource.PLAYERS,
            0.8F,
            2.0F / ((float) charge / this.getNumCharges(eye) + 2.0F));
        WorldHelper.createLootDrop(drops, player.level(), startingPos);
      }

      return InteractionResult.CONSUME;
    } else {
      this.showFailure(player, PELang.MERCURIAL_EYE_UNPLACEABLE_TARGET);
      return InteractionResult.FAIL;
    }
  }

  private boolean doBlockPlace(
      Player player,
      Level level,
      BlockState oldState,
      BlockPos placePos,
      BlockState newState,
      ItemStack eye,
      long oldEMC,
      long newEMC,
      NonNullList<ItemStack> drops,
      BlockPlaceContext context,
      ActionFeedback feedback) {
    if (oldState.hasBlockEntity()) {
      return false;
    }

    if (!newState.getBlock().getStateDefinition().getProperties().isEmpty()) {
      BlockPlaceContext adjustedContext =
          BlockPlaceContext.at(context, placePos, context.getClickedFace());
      ((BlockPlaceContextAccessor) adjustedContext).projecte$setReplaceClicked(true);
      newState = newState.getBlock().getStateForPlacement(adjustedContext);
      if (newState == null) {
        return false;
      }
    }

    return this.doBlockPlace(
        player, level, oldState, placePos, newState, eye, oldEMC, newEMC, drops, feedback);
  }

  private boolean doBlockPlace(
      Player player,
      Level level,
      BlockState oldState,
      BlockPos placePos,
      BlockState newState,
      ItemStack eye,
      long oldEMC,
      long newEMC,
      NonNullList<ItemStack> drops,
      ActionFeedback feedback) {
    if (oldState != newState && !oldState.hasBlockEntity()) {
      IItemHandler inventory = Capabilities.ItemHandler.ITEM.find(eye, null);
      if (inventory == null) {
        return false;
      }

      ItemStack klein = inventory.getStackInSlot(0);
      IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(klein, null);
      if (emcHolder == null || !(inventory instanceof IItemHandlerModifiable modifiable)) {
        if (!feedback.failureShown) {
          this.showFailure(player, PELang.EMC_INSUFFICIENT);
          feedback.failureShown = true;
        }
        return false;
      }

      if (oldEMC == 0L && oldState.getDestroySpeed(level, placePos) == -1.0F) {
        return false;
      }

      ItemStack replacement = klein.copy();
      if (oldEMC > newEMC) {
        long refund = oldEMC - newEMC;
        if (emcHolder.insertEmc(replacement, refund, IEmcStorage.EmcAction.EXECUTE) != refund) {
          if (!feedback.failureShown) {
            this.showFailure(player, PELang.EMC_STORAGE_FULL);
            feedback.failureShown = true;
          }
          return false;
        }
      } else if (oldEMC != newEMC) {
        long cost = newEMC - oldEMC;
        if (emcHolder.extractEmc(replacement, cost, IEmcStorage.EmcAction.EXECUTE) != cost) {
          if (!feedback.failureShown) {
            this.showFailure(player, PELang.EMC_INSUFFICIENT);
            feedback.failureShown = true;
          }
          return false;
        }
      }

      ServerPlayer serverPlayer = (ServerPlayer) player;
      if (PlayerHelper.checkedReplaceBlock(serverPlayer, level, placePos, newState)) {
        if (oldEMC == 0L && oldEMC != newEMC) {
          WorldHelper.collectBlockDrops(
              oldState, (ServerLevel) level, placePos, null, player, eye, drops);
        }
        modifiable.setStackInSlot(0, replacement);

        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  private int fillGaps(
      ItemStack eye,
      Player player,
      Level level,
      BlockPlaceContext context,
      BlockState startingState,
      BlockState newState,
      long newBlockEmc,
      AABB bounds,
      NonNullList<ItemStack> drops,
      ActionFeedback feedback) {
    int hitTargets = 0;

    for (BlockPos pos : WorldHelper.getPositionsInBox(bounds)) {
      VoxelShape bb = startingState.getCollisionShape(level, pos);
      if (level.isUnobstructed(null, bb)) {
        BlockPlaceContext adjustedContext =
            BlockPlaceContext.at(context, pos, context.getClickedFace());
        if (adjustedContext.replacingClickedOnBlock()) {
          BlockState placeState = level.getBlockState(pos);
          long placeBlockEmc = IEMCProxy.INSTANCE.getValue(placeState.getBlock());
          BlockState stateToPlace = newState;
          if (!newState.getBlock().getStateDefinition().getProperties().isEmpty()) {
            stateToPlace = newState.getBlock().getStateForPlacement(adjustedContext);
            if (stateToPlace == null) {
              continue;
            }
          }

          if (this.doBlockPlace(
              player,
              level,
              placeState,
              pos.immutable(),
              stateToPlace,
              eye,
              placeBlockEmc,
              newBlockEmc,
              drops,
              feedback)) {
            hitTargets++;
          }
        }
      }
    }

    return hitTargets;
  }

  private static class ActionFeedback {
    private boolean failureShown;
  }

  private AABB getBounds(
      BlockPos startingPos, @Nullable Direction facing, int strength, int depth) {
    return facing == null
        ? new AABB(startingPos)
        : WorldHelper.getBroadDeepBox(startingPos, facing, strength, depth);
  }

  @Override
  public void attachCapabilities(RegisterCapabilitiesEvent event) {
    event.registerItem(
        Capabilities.ItemHandler.ITEM,
        (stack, context) -> new MercurialEye.EyeItemHandler(stack),
        this);
  }

  @Override
  public DataComponentType<MercurialEye.MercurialEyeMode> getDataComponentType() {
    return PEDataComponentTypes.MERCURIAL_EYE_MODE.get();
  }

  public MercurialEye.MercurialEyeMode getDefaultMode() {
    return MercurialEye.MercurialEyeMode.CREATION;
  }

  private static class EyeItemHandler extends ComponentItemHandler {
    public EyeItemHandler(ItemStack parent) {
      super(parent, PEDataComponentTypes.EYE_INVENTORY.get(), 2);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
      if (stack.isEmpty()) {
        return true;
      } else {
        return slot == 0
            ? SlotPredicates.EMC_HOLDER.test(stack)
            : SlotPredicates.MERCURIAL_TARGET.test(stack);
      }
    }

    @Override
    public int getSlotLimit(int slot) {
      return 1;
    }

    @Override
    protected void updateContents(
        @NotNull ItemContainerContents contents, @NotNull ItemStack stack, int slot) {
      super.updateContents(contents, stack.copyWithCount(1), slot);
    }
  }

  public enum MercurialEyeMode implements IModeEnum<MercurialEye.MercurialEyeMode> {
    CREATION(PELang.MODE_MERCURIAL_EYE_1),
    EXTENSION(PELang.MODE_MERCURIAL_EYE_2),
    EXTENSION_CLASSIC(PELang.MODE_MERCURIAL_EYE_3),
    TRANSMUTATION(PELang.MODE_MERCURIAL_EYE_4),
    TRANSMUTATION_CLASSIC(PELang.MODE_MERCURIAL_EYE_5),
    PILLAR(PELang.MODE_MERCURIAL_EYE_6);

    public static final Codec<MercurialEye.MercurialEyeMode> CODEC =
        StringRepresentable.fromEnum(MercurialEye.MercurialEyeMode::values);
    public static final IntFunction<MercurialEye.MercurialEyeMode> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, MercurialEye.MercurialEyeMode> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final IHasTranslationKey langEntry;
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    MercurialEyeMode(IHasTranslationKey langEntry) {
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

    public boolean isExtension() {
      return this == EXTENSION || this == EXTENSION_CLASSIC;
    }

    public MercurialEye.MercurialEyeMode next(ItemStack stack) {
      return switch (this) {
        case CREATION -> EXTENSION;
        case EXTENSION -> EXTENSION_CLASSIC;
        case EXTENSION_CLASSIC -> TRANSMUTATION;
        case TRANSMUTATION -> TRANSMUTATION_CLASSIC;
        case TRANSMUTATION_CLASSIC -> PILLAR;
        case PILLAR -> CREATION;
      };
    }
  }
}
