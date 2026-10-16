package moze_intel.projecte.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public final class LightningHelper {
  private static final Identifier LIGHTNING_BOLT_ID =
      Identifier.fromNamespaceAndPath("minecraft", "lightning_bolt");

  private LightningHelper() {}

  public static LightningBolt create(Level level) {
    EntityType<? extends LightningBolt> type =
        (EntityType<? extends LightningBolt>)
            BuiltInRegistries.ENTITY_TYPE.getValue(LIGHTNING_BOLT_ID);
    if (type == null) {
      throw new IllegalStateException("Missing vanilla entity type minecraft:lightning_bolt");
    } else {
      return new LightningBolt(type, level);
    }
  }
}
