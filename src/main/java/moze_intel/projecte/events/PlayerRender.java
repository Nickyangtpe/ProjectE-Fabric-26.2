package moze_intel.projecte.events;

import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;

public final class PlayerRender {
  private PlayerRender() {}

  public static float applyGemBootsFovModifier(float vanillaModifier) {
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft.getCameraEntity() instanceof AbstractClientPlayer player
            && player.getItemBySlot(EquipmentSlot.FEET).is(PEItems.GEM_BOOTS.get())
        ? vanillaModifier - 0.5F * ((Double) minecraft.options.fovEffectScale().get()).floatValue()
        : vanillaModifier;
  }
}
