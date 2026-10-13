package moze_intel.projecte.compat.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public final class ItemAttributeModifierEvent {
  private final ItemStack itemStack;
  private final List<ItemAttributeModifierEvent.Modifier> modifiers = new ArrayList<>();

  public ItemAttributeModifierEvent(ItemStack itemStack) {
    this.itemStack = itemStack;
  }

  public ItemStack getItemStack() {
    return this.itemStack;
  }

  public void addModifier(
      Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slotGroup) {
    this.modifiers.add(new ItemAttributeModifierEvent.Modifier(attribute, modifier, slotGroup));
  }

  public List<ItemAttributeModifierEvent.Modifier> getModifiers() {
    return Collections.unmodifiableList(this.modifiers);
  }

  public record Modifier(
      Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slotGroup) {}
}
