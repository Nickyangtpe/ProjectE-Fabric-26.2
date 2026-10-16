package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.common.ItemAbilities;
import moze_intel.projecte.compat.common.ToolActionHelper;
import moze_intel.projecte.compat.fluids.FluidStack;
import moze_intel.projecte.compat.fluids.capability.IFluidHandler;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.utils.WorldHelper;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class DispenserBehaviors {
  private DispenserBehaviors() {}

  public static void register() {
    DispenseItemBehavior shears = new ShearsDispenseItemBehavior();
    register(
        shears, PEItems.DARK_MATTER_SHEARS, PEItems.RED_MATTER_SHEARS, PEItems.RED_MATTER_KATAR);
    DispenserBlock.registerBehavior(
        PEBlocks.NOVA_CATALYST,
        ((ProjectETNT) PEBlocks.NOVA_CATALYST.getBlock()).createDispenseItemBehavior());
    DispenserBlock.registerBehavior(
        PEBlocks.NOVA_CATACLYSM,
        ((ProjectETNT) PEBlocks.NOVA_CATACLYSM.getBlock()).createDispenseItemBehavior());
    DispenseItemBehavior ignition =
        new OptionalDispenseItemBehavior() {
          protected ItemStack execute(BlockSource source, ItemStack stack) {
            if (!ItemAbilities.canPerformAction(stack, ItemAbilities.FIRESTARTER_LIGHT)) {
              this.setSuccess(false);
              return super.execute(source, stack);
            }

            Level level = source.level();
            Direction direction = (Direction) source.state().getValue(DispenserBlock.FACING);
            BlockPos pos = source.pos().relative(direction);
            BlockState state = level.getBlockState(pos);
            this.setSuccess(true);
            if (BaseFireBlock.canBePlacedAt(level, pos, direction)) {
              level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
              level.gameEvent(null, GameEvent.BLOCK_PLACE, pos);
            } else {
              Direction hitFace = direction.getOpposite();
              BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), hitFace, pos, false);
              UseOnContext context =
                  new UseOnContext(level, null, InteractionHand.MAIN_HAND, stack, hit);
              BlockState modifiedState =
                  ToolActionHelper.getToolModifiedState(
                      state, context, ItemAbilities.FIRESTARTER_LIGHT, false);
              if (modifiedState != null) {
                level.setBlockAndUpdate(pos, modifiedState);
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
              } else if (state.getBlock() instanceof TntBlock) {
                TntBlock.prime(level, pos);
              } else {
                Entry entry =
                    (Entry) FlammableBlockRegistry.getDefaultInstance().get(state.getBlock());
                boolean flammable =
                    state.ignitedByLava() || entry != null && entry.getIgniteOdds() > 0;
                BlockPos firePos = pos.relative(direction);
                if (flammable && BaseFireBlock.canBePlacedAt(level, firePos, direction)) {
                  level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                  level.gameEvent(null, GameEvent.BLOCK_PLACE, firePos);
                } else {
                  this.setSuccess(false);
                }
              }
            }

            return stack;
          }
        };
    register(ignition, PEItems.IGNITION_RING, PEItems.ARCANA_RING);
    DispenserBlock.registerBehavior(
        PEItems.EVERTIDE_AMULET,
        new DefaultDispenseItemBehavior() {
          protected ItemStack execute(BlockSource source, ItemStack stack) {
            Level level = source.level();
            Direction direction = (Direction) source.state().getValue(DispenserBlock.FACING);
            BlockPos pos = source.pos().relative(direction);
            IFluidHandler fluidHandler =
                WorldHelper.getCapability(
                    level, Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite());
            if (fluidHandler != null) {
              fluidHandler.fill(
                  new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
              return stack;
            } else {
              WorldHelper.placeFluid(
                  null, level, pos, Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
              level.playSound(
                  null,
                  pos.getX(),
                  pos.getY(),
                  pos.getZ(),
                  PESoundEvents.WATER_MAGIC.get(),
                  SoundSource.PLAYERS,
                  1.0F,
                  1.0F);
              return stack;
            }
          }
        });
  }

  private static void register(DispenseItemBehavior behavior, ItemLike... items) {
    for (ItemLike item : items) {
      DispenserBlock.registerBehavior(item, behavior);
    }
  }
}
