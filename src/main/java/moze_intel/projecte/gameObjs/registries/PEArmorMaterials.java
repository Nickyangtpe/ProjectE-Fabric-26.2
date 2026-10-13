package moze_intel.projecte.gameObjs.registries;

import java.util.Map;
import moze_intel.projecte.PECore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class PEArmorMaterials {
  public static final int BASE_DURABILITY = 33;
  private static final Map<ArmorType, Integer> DIAMOND_RESISTANCES =
      Map.of(
          ArmorType.BOOTS,
          3,
          ArmorType.LEGGINGS,
          6,
          ArmorType.CHESTPLATE,
          8,
          ArmorType.HELMET,
          3,
          ArmorType.BODY,
          11);
  private static final TagKey<Item> NO_REPAIR_INGREDIENT =
      TagKey.create(Registries.ITEM, PECore.rl("never_repair"));
  public static final ArmorMaterial DARK_MATTER = material(2.0F, 0.1F, "dark_matter");
  public static final ArmorMaterial RED_MATTER = material(2.0F, 0.2F, "red_matter");
  public static final ArmorMaterial GEM_ARMOR = material(2.0F, 0.25F, "gem_armor");

  private PEArmorMaterials() {}

  private static ResourceKey<EquipmentAsset> asset(String name) {
    return ResourceKey.create(EquipmentAssets.ROOT_ID, PECore.rl(name));
  }

  private static ArmorMaterial material(
      float toughness, float knockbackResistance, String assetName) {
    return new ArmorMaterial(
        33,
        DIAMOND_RESISTANCES,
        0,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        toughness,
        knockbackResistance,
        NO_REPAIR_INGREDIENT,
        asset(assetName));
  }
}
