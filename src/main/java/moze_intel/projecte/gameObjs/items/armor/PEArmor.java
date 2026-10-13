package moze_intel.projecte.gameObjs.items.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public abstract class PEArmor extends Item {
  private final ArmorType armorType;

  protected PEArmor(ArmorMaterial material, ArmorType armorType, Properties props) {
    this(material, armorType, props, material.createAttributes(armorType));
  }

  protected PEArmor(
      ArmorMaterial material,
      ArmorType armorType,
      Properties props,
      ItemAttributeModifiers attributes) {
    super(
        props
            .humanoidArmor(material, armorType)
            .durability(armorType.getDurability(33))
            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
            .attributes(attributes));
    this.armorType = armorType;
  }

  protected final ArmorType armorType() {
    return this.armorType;
  }

  public abstract float getFullSetBaseReduction();

  public abstract float getMaxDamageAbsorb(ArmorType var1, DamageSource var2);

  public PEArmor.ReductionInfo getReductionInfo(DamageSource source) {
    float maxDamageAbsorb = this.getMaxDamageAbsorb(this.armorType, source);
    float fullSetReduction = this.getFullSetBaseReduction();
    if (maxDamageAbsorb > 0.0F && fullSetReduction > 0.0F) {
      float pieceEffectiveness = this.getPieceEffectiveness(this.armorType);
      return new PEArmor.ReductionInfo(
          fullSetReduction * pieceEffectiveness, maxDamageAbsorb * pieceEffectiveness);
    } else {
      return PEArmor.ReductionInfo.ZERO;
    }
  }

  public float getPieceEffectiveness(ArmorType type) {
    if (type == ArmorType.BOOTS || type == ArmorType.HELMET) {
      return 0.2F;
    } else {
      return type != ArmorType.CHESTPLATE && type != ArmorType.LEGGINGS ? 0.0F : 0.3F;
    }
  }

  protected static boolean isArmorSlot(int slot) {
    return slot >= 36 && slot < 40;
  }

  protected static boolean isArmorSlot(EquipmentSlot slot) {
    return slot == EquipmentSlot.HEAD
        || slot == EquipmentSlot.CHEST
        || slot == EquipmentSlot.LEGS
        || slot == EquipmentSlot.FEET;
  }

  public record ReductionInfo(float percentReduced, float maxDamagedAbsorbed) {
    public static final PEArmor.ReductionInfo ZERO = new PEArmor.ReductionInfo(0.0F, 0.0F);

    public PEArmor.ReductionInfo add(PEArmor.ReductionInfo other) {
      if (this == ZERO) {
        return other;
      } else {
        return other == ZERO
            ? this
            : new PEArmor.ReductionInfo(
                this.percentReduced + other.percentReduced,
                this.maxDamagedAbsorbed + other.maxDamagedAbsorbed);
      }
    }
  }
}
