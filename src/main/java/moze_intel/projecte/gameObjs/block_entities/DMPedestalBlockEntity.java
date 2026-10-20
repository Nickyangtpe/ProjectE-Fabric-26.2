package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.compat.capabilities.ICapabilityProvider;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DMPedestalBlockEntity extends EmcBlockEntity implements IDMPedestal {
  public static final ICapabilityProvider<DMPedestalBlockEntity, @Nullable Direction, IItemHandler>
      INVENTORY_PROVIDER = (pedestal, side) -> pedestal.inventory;
  private static final int RANGE = 4;
  private final EmcBlockEntity.StackHandler inventory =
      new EmcBlockEntity.StackHandler(1) {
        @Override
        public void onContentsChanged(int slot) {
          super.onContentsChanged(slot);
          if (DMPedestalBlockEntity.this.level != null
              && !DMPedestalBlockEntity.this.level.isClientSide()) {
            BlockState state = DMPedestalBlockEntity.this.getBlockState();
            DMPedestalBlockEntity.this.level.sendBlockUpdated(
                DMPedestalBlockEntity.this.worldPosition, state, state, Block.UPDATE_IMMEDIATE);
          }
        }
      };
  private boolean isActive = false;
  private int particleCooldown = 10;
  private int activityCooldown = 0;
  public boolean previousRedstoneState = false;

  public DMPedestalBlockEntity(BlockPos pos, BlockState state) {
    super(PEBlockEntityTypes.DARK_MATTER_PEDESTAL, pos, state, 1000L);
  }

  public static void tickClient(
      Level level, BlockPos pos, BlockState state, DMPedestalBlockEntity pedestal) {
    if (pedestal.getActive()) {
      ItemStack stack = pedestal.inventory.getStackInSlot(0);
      IPedestalItem pedestalItem = PECapabilities.PEDESTAL_ITEM_CAPABILITY.find(stack, null);
      if (pedestalItem == null) {
        pedestal.setActive(level, pos, false);
      } else {
        pedestalItem.updateInPedestal(stack, level, pos, pedestal);
        if (pedestal.particleCooldown <= 0) {
          spawnParticleTypes(level, pos);
          pedestal.particleCooldown = 10;
        } else {
          pedestal.particleCooldown--;
        }
      }
    }
  }

  public static void tickServer(
      Level level, BlockPos pos, BlockState state, DMPedestalBlockEntity pedestal) {
    if (pedestal.getActive()) {
      ItemStack stack = pedestal.inventory.getStackInSlot(0);
      IPedestalItem pedestalItem = PECapabilities.PEDESTAL_ITEM_CAPABILITY.find(stack, null);
      if (pedestalItem == null) {
        pedestal.setActive(level, pos, false);
      } else if (pedestalItem.updateInPedestal(stack, level, pos, pedestal)) {
        pedestal.inventory.onContentsChanged(0);
      }
    }

    pedestal.updateComparators(level, pos);
  }

  private static void spawnParticleTypes(@NotNull Level level, @NotNull BlockPos pos) {
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    level.addParticle(ParticleTypes.FLAME, x + 0.2, y + 0.3, z + 0.2, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.2, y + 0.3, z + 0.5, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.2, y + 0.3, z + 0.8, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.5, y + 0.3, z + 0.2, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.5, y + 0.3, z + 0.8, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.8, y + 0.3, z + 0.2, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.8, y + 0.3, z + 0.5, 0.0, 0.0, 0.0);
    level.addParticle(ParticleTypes.FLAME, x + 0.8, y + 0.3, z + 0.8, 0.0, 0.0, 0.0);
    RandomSource rand = level.getRandom();

    for (int i = 0; i < 3; i++) {
      int xDirection = rand.nextBoolean() ? -1 : 1;
      int zDirection = rand.nextBoolean() ? -1 : 1;
      level.addParticle(
          ParticleTypes.PORTAL,
          x + 0.5 + xDirection * 0.25,
          y + rand.nextFloat(),
          z + 0.5 + zDirection * 0.25,
          xDirection * rand.nextFloat(),
          (rand.nextFloat() - 0.5) * 0.125,
          zDirection * rand.nextFloat());
    }
  }

  @Override
  public int getActivityCooldown() {
    return this.activityCooldown;
  }

  @Override
  public void setActivityCooldown(@NotNull Level level, @NotNull BlockPos pos, int cooldown) {
    if (this.activityCooldown != cooldown) {
      this.activityCooldown = cooldown;
      this.markDirty(level, pos, false);
    }
  }

  @Override
  public void decrementActivityCooldown(@NotNull Level level, @NotNull BlockPos pos) {
    this.activityCooldown--;
    this.markDirty(level, pos, false);
  }

  @Override
  public AABB getEffectBounds() {
    return new AABB(this.worldPosition).inflate(4.0);
  }

  @Override
  public void loadAdditional(@NotNull ValueInput tag) {
    super.loadAdditional(tag);
    CompoundTag data =
        tag.read("inventory", CompoundTag.CODEC)
            .orElseGet(
                () -> {
                  CompoundTag legacy = new CompoundTag();
                  legacy.putInt("Size", tag.getIntOr("Size", this.inventory.getSlots()));
                  ListTag items = new ListTag();
                  tag.list("Items", CompoundTag.CODEC)
                      .ifPresent(entries -> entries.stream().forEach(items::add));
                  legacy.put("Items", items);
                  return legacy;
                });
    this.inventory.deserializeNBT(tag.lookup(), data);
    this.isActive = tag.getBooleanOr("active", false);
    this.activityCooldown = tag.getIntOr("activity_cooldown", 0);
    this.previousRedstoneState = tag.getBooleanOr("powered", false);
  }

  @Override
  protected void saveAdditional(@NotNull ValueOutput tag) {
    super.saveAdditional(tag);
    tag.store(
        "inventory", CompoundTag.CODEC, this.inventory.serializeNBT(this.level.registryAccess()));
    tag.putBoolean("active", this.getActive());
    tag.putInt("activity_cooldown", this.activityCooldown);
    tag.putBoolean("powered", this.previousRedstoneState);
  }

  public boolean getActive() {
    return this.isActive;
  }

  public void setActive(@NotNull Level level, @NotNull BlockPos pos, boolean newState) {
    if (newState != this.getActive()) {
      if (newState) {
        level.playSound(null, pos, PESoundEvents.CHARGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        spawnToggleParticles(level, pos, ParticleTypes.WITCH);
      } else {
        level.playSound(null, pos, PESoundEvents.UNCHARGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        spawnToggleParticles(level, pos, ParticleTypes.SMOKE);
      }
    }

    this.isActive = newState;
    this.markDirty(level, pos, true);
  }

  /**
   * Sends the activation burst to clients when called on the logical server.
   * Level#addParticle is client-local in the Fabric port, so using it from a
   * server-side interaction silently loses the original ProjectE effect.
   */
  private static void spawnToggleParticles(
      @NotNull Level level, @NotNull BlockPos pos, @NotNull net.minecraft.core.particles.ParticleOptions type) {
    int count = level.getRandom().nextInt(35) + 10;
    for (int i = 0; i < count; i++) {
      double x = pos.getX() + 0.5 + level.getRandom().nextGaussian() * 0.13F;
      double y = pos.getY() + 1 + level.getRandom().nextGaussian() * 0.13F;
      double z = pos.getZ() + 0.5 + level.getRandom().nextGaussian() * 0.13F;
      if (level.isClientSide()) {
        level.addParticle(type, x, y, z, 0.0, 0.0, 0.0);
      } else if (level instanceof ServerLevel serverLevel) {
        serverLevel.sendParticles(type, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }
  }

  public IItemHandlerModifiable getInventory() {
    return this.inventory;
  }
}
