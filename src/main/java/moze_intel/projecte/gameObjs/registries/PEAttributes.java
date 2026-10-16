package moze_intel.projecte.gameObjs.registries;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.ModifyDefaultAttribute;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class PEAttributes {
  public static final Holder<Attribute> CREATIVE_FLIGHT =
      Registry.registerForHolder(
          BuiltInRegistries.ATTRIBUTE,
          Identifier.fromNamespaceAndPath("projecte", "creative_flight"),
          new RangedAttribute("attribute.name.projecte.creative_flight", 0.0, 0.0, 1.0)
              .setSyncable(true));
  private static final EntityType<? extends LivingEntity> PLAYER_TYPE = playerType();
  private static boolean initialized;

  private PEAttributes() {}

  private static EntityType<? extends LivingEntity> playerType() {
    EntityType<?> type =
        (EntityType<?>)
            BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "player"));
    if (type == null) {
      throw new IllegalStateException("Missing vanilla entity type minecraft:player");
    } else {
      return (EntityType<? extends LivingEntity>) type;
    }
  }

  public static synchronized void initialize() {
    if (!initialized) {
      initialized = true;
      FabricDefaultAttributeRegistry.MODIFY.register(
          (ModifyDefaultAttribute)
              context ->
                  context.modify(
                      PLAYER_TYPE, (entityType, builder) -> builder.add(CREATIVE_FLIGHT, 0.0)));
    }
  }

  public static void tickFlight(ServerPlayer player) {
    boolean hasFlightAttribute = player.getAttributeValue(CREATIVE_FLIGHT) > 0.0;
    if (hasFlightAttribute) {
      if (!player.hasAttached(PEAttachmentTypes.SWIFTWOLF_FLIGHT)) {
        player.setAttached(PEAttachmentTypes.SWIFTWOLF_FLIGHT, player.getAbilities().mayfly);
      }

      if (!player.getAbilities().mayfly) {
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
      }
    } else if (player.hasAttached(PEAttachmentTypes.SWIFTWOLF_FLIGHT)) {
      boolean couldFlyBeforeProjectE =
          (Boolean) player.getAttachedOrThrow(PEAttachmentTypes.SWIFTWOLF_FLIGHT);
      player.removeAttached(PEAttachmentTypes.SWIFTWOLF_FLIGHT);
      if (!couldFlyBeforeProjectE && !player.isCreative() && !player.isSpectator()) {
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
      }
    }
  }
}
