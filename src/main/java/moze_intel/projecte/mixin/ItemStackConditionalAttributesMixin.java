package moze_intel.projecte.mixin;

import java.util.function.BiConsumer;
import moze_intel.projecte.compat.event.ItemAttributeModifierEvent;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers.Display;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackConditionalAttributesMixin {
  @Inject(
      method =
          "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
      at = @At("TAIL"))
  private void projecte$applyConditionalModifiers(
      EquipmentSlot slot,
      BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
      CallbackInfo ci) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof IHasConditionalAttributes conditional) {
      ItemAttributeModifierEvent event = new ItemAttributeModifierEvent(stack);
      conditional.adjustAttributes(event);

      for (ItemAttributeModifierEvent.Modifier modifier : event.getModifiers()) {
        if (modifier.slotGroup().test(slot)) {
          consumer.accept(modifier.attribute(), modifier.modifier());
        }
      }
    }
  }

  @Inject(
      method =
          "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
      at = @At("TAIL"))
  private void projecte$applyConditionalModifiersForTooltip(
      EquipmentSlotGroup slotGroup,
      TriConsumer<Holder<Attribute>, AttributeModifier, Display> consumer,
      CallbackInfo ci) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof IHasConditionalAttributes conditional) {
      ItemAttributeModifierEvent event = new ItemAttributeModifierEvent(stack);
      conditional.adjustAttributes(event);

      for (ItemAttributeModifierEvent.Modifier modifier : event.getModifiers()) {
        if (slotGroup.slots().stream().anyMatch(modifier.slotGroup()::test)) {
          consumer.accept(modifier.attribute(), modifier.modifier(), Display.attributeModifiers());
        }
      }
    }
  }
}
