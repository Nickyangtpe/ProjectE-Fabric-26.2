package moze_intel.projecte.mixin;

import moze_intel.projecte.compat.capabilities.CapabilityHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

public final class CapabilityMixin {
  private CapabilityMixin() {}

  @Mixin(BlockEntity.class)
  public abstract static class BlockEntityMixin implements CapabilityHolder.BlockEntityAccess {}

  @Mixin(Entity.class)
  public abstract static class EntityMixin implements CapabilityHolder.EntityAccess {}

  @Mixin(ItemStack.class)
  public abstract static class ItemStackMixin implements CapabilityHolder.ItemStackAccess {}

  @Mixin(Level.class)
  public abstract static class LevelMixin implements CapabilityHolder.LevelAccess {}
}
