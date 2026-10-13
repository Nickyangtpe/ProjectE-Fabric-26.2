package moze_intel.projecte.api.capabilities;

import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.compat.capabilities.BlockCapability;
import moze_intel.projecte.compat.capabilities.EntityCapability;
import moze_intel.projecte.compat.capabilities.ItemCapability;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class PECapabilities {
  public static final BlockCapability<IEmcStorage, @Nullable Direction> EMC_STORAGE_CAPABILITY =
      BlockCapability.createSided(rl("emc_storage"), IEmcStorage.class);
  public static final EntityCapability<IAlchBagProvider, Void> ALCH_BAG_CAPABILITY =
      EntityCapability.createVoid(rl("alchemical_bag"), IAlchBagProvider.class);
  public static final EntityCapability<IKnowledgeProvider, Void> KNOWLEDGE_CAPABILITY =
      EntityCapability.createVoid(rl("knowledge"), IKnowledgeProvider.class);
  public static final ItemCapability<IAlchBagItem, Void> ALCH_BAG_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("alchemical_bag"), IAlchBagItem.class);
  public static final ItemCapability<IAlchChestItem, Void> ALCH_CHEST_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("alchemical_chest"), IAlchChestItem.class);
  public static final ItemCapability<IExtraFunction, Void> EXTRA_FUNCTION_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("extra_function"), IExtraFunction.class);
  public static final ItemCapability<IItemCharge, Void> CHARGE_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("charge"), IItemCharge.class);
  public static final ItemCapability<IItemEmcHolder, Void> EMC_HOLDER_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("emc_holder"), IItemEmcHolder.class);
  public static final ItemCapability<IModeChanger<?>, Void> MODE_CHANGER_ITEM_CAPABILITY =
      (ItemCapability<IModeChanger<?>, Void>)
          (ItemCapability<?, ?>)
              ItemCapability.createVoid(rl("mode_changer"), (Class) IModeChanger.class);
  public static final ItemCapability<IPedestalItem, Void> PEDESTAL_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("pedestal"), IPedestalItem.class);
  public static final ItemCapability<IProjectileShooter, Void> PROJECTILE_SHOOTER_ITEM_CAPABILITY =
      ItemCapability.createVoid(rl("projectile_shooter"), IProjectileShooter.class);

  private PECapabilities() {}

  private static Identifier rl(String path) {
    return Identifier.fromNamespaceAndPath("projecte", path);
  }
}
