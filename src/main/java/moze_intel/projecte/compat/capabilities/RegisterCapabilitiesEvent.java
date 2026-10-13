package moze_intel.projecte.compat.capabilities;

import moze_intel.projecte.compat.eventbus.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegisterCapabilitiesEvent extends Event {
  public <T, C, BE extends BlockEntity> void registerBlockEntity(
      BlockCapability<T, C> capability,
      BlockEntityType<BE> type,
      ICapabilityProvider<BE, C, T> provider) {
    capability.registerForBlockEntity(provider, type);
  }

  public <T, C, E extends Entity> void registerEntity(
      EntityCapability<T, C> capability,
      EntityType<E> type,
      ICapabilityProvider<E, C, T> provider) {
    capability.registerForType(provider, type);
  }

  public <T, C> void registerItem(
      ItemCapability<T, C> capability,
      ICapabilityProvider<ItemStack, C, T> provider,
      Item... items) {
    capability.registerForItems(provider, items);
  }
}
