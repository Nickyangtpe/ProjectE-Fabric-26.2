package moze_intel.projecte.gameObjs.items.armor;

import moze_intel.projecte.gameObjs.registries.PEArmorMaterials;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.equipment.ArmorType;

public class RMArmor extends PEArmor {
  public RMArmor(ArmorType armorType, Properties props) {
    super(PEArmorMaterials.RED_MATTER, armorType, props);
  }

  @Override
  public float getFullSetBaseReduction() {
    return 0.9F;
  }

  @Override
  public float getMaxDamageAbsorb(ArmorType type, DamageSource source) {
    if (source.is(DamageTypeTags.IS_EXPLOSION)) {
      return 500.0F;
    } else if (type == ArmorType.BOOTS && source.is(DamageTypeTags.IS_FALL)) {
      return 10.0F / this.getPieceEffectiveness(type);
    } else if (type == ArmorType.HELMET && source.is(DamageTypeTags.IS_DROWNING)) {
      return 10.0F / this.getPieceEffectiveness(type);
    } else if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
      return 0.0F;
    } else {
      return type != ArmorType.HELMET && type != ArmorType.BOOTS ? 350.0F : 250.0F;
    }
  }
}
