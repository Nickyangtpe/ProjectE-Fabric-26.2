package moze_intel.projecte.events;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.world.phys.HitResult.Type;

public final class ArchangelSmiteClientEvents {
  private ArchangelSmiteClientEvents() {}

  public static void register() {
    ClientPreAttackCallback.EVENT.register(
        (ClientPreAttackCallback)
            (client, player, clickCount) -> {
              if (client.hitResult != null
                  && client.hitResult.getType() == Type.MISS
                  && player.getMainHandItem().is(PEItems.ARCHANGEL_SMITE.get())) {
                PECore.packetHandler().activateArchangel();
              }

              return false;
            });
  }
}
