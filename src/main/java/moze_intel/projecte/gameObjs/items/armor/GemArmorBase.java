package moze_intel.projecte.gameObjs.items.armor;

import moze_intel.projecte.gameObjs.registries.PEArmorMaterials;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorType;

public abstract class GemArmorBase extends PEArmor {
  public GemArmorBase(ArmorType armorType, Properties props) {
    super(PEArmorMaterials.GEM_ARMOR, armorType, props);
  }

  public GemArmorBase(ArmorType armorType, Properties props, ItemAttributeModifiers attributes) {
    super(PEArmorMaterials.GEM_ARMOR, armorType, props, attributes);
  }

  @Override
  public float getFullSetBaseReduction() {
    return 0.9F;
  }

  @Override
  public float getMaxDamageAbsorb(ArmorType type, DamageSource source) {
    if (source.is(DamageTypeTags.IS_EXPLOSION)) {
      return 750.0F;
    } else if (type == ArmorType.BOOTS && source.is(DamageTypeTags.IS_FALL)) {
      return 15.0F / this.getPieceEffectiveness(type);
    } else if (type == ArmorType.HELMET && source.is(DamageTypeTags.IS_DROWNING)) {
      return 15.0F / this.getPieceEffectiveness(type);
    } else if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
      return 0.0F;
    } else {
      return type != ArmorType.HELMET && type != ArmorType.BOOTS ? 500.0F : 400.0F;
    }
  }

  public static boolean hasAnyPiece(Player player) {
    return player.getItemBySlot(EquipmentSlot.HEAD).is(PEItems.GEM_HELMET.get())
        || player.getItemBySlot(EquipmentSlot.CHEST).is(PEItems.GEM_CHESTPLATE.get())
        || player.getItemBySlot(EquipmentSlot.LEGS).is(PEItems.GEM_LEGGINGS.get())
        || player.getItemBySlot(EquipmentSlot.FEET).is(PEItems.GEM_BOOTS.get());
  }

  public static boolean hasFullSet(Player player) {
    return player.getItemBySlot(EquipmentSlot.HEAD).is(PEItems.GEM_HELMET.get())
        && player.getItemBySlot(EquipmentSlot.CHEST).is(PEItems.GEM_CHESTPLATE.get())
        && player.getItemBySlot(EquipmentSlot.LEGS).is(PEItems.GEM_LEGGINGS.get())
        && player.getItemBySlot(EquipmentSlot.FEET).is(PEItems.GEM_BOOTS.get());
  }
}
