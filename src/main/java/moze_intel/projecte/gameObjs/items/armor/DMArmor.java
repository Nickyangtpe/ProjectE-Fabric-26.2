package moze_intel.projecte.gameObjs.items.armor;

import moze_intel.projecte.gameObjs.registries.PEArmorMaterials;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.equipment.ArmorType;

public class DMArmor extends PEArmor {
  public DMArmor(ArmorType armorPiece, Properties props) {
    super(PEArmorMaterials.DARK_MATTER, armorPiece, props);
  }

  @Override
  public float getFullSetBaseReduction() {
    return 0.8F;
  }

  @Override
  public float getMaxDamageAbsorb(ArmorType type, DamageSource source) {
    if (source.is(DamageTypeTags.IS_EXPLOSION)) {
      return 350.0F;
    } else if (type == ArmorType.BOOTS && source.is(DamageTypeTags.IS_FALL)) {
      return 5.0F / this.getPieceEffectiveness(type);
    } else if (type == ArmorType.HELMET && source.is(DamageTypeTags.IS_DROWNING)) {
      return 5.0F / this.getPieceEffectiveness(type);
    } else if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
      return 0.0F;
    } else {
      return type != ArmorType.HELMET && type != ArmorType.BOOTS ? 150.0F : 100.0F;
    }
  }
}
