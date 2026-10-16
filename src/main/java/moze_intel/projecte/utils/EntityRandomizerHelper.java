package moze_intel.projecte.utils;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.PETags;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.rabbit.Rabbit.Variant;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EntityRandomizerHelper {
  @Nullable
  public static Mob getRandomEntity(Level level, Mob toRandomize) {
    EntityType<?> entType = toRandomize.getType();
    boolean isPeaceful =
        BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entType).is(PETags.Entities.RANDOMIZER_PEACEFUL);
    boolean isHostile =
        BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entType).is(PETags.Entities.RANDOMIZER_HOSTILE);
    if (isPeaceful
        && isHostile
        && toRandomize instanceof Rabbit rabbit
        && rabbit.getVariant() == Variant.EVIL) {
      isPeaceful = false;
    }

    if (isPeaceful) {
      return createRandomEntity(level, toRandomize, PETags.Entities.RANDOMIZER_PEACEFUL);
    }

    if (isHostile) {
      Mob ent = createRandomEntity(level, toRandomize, PETags.Entities.RANDOMIZER_HOSTILE);
      if (ent instanceof Rabbit rabbit) {
        rabbit.setVariant(Variant.EVIL);
      }

      return ent;
    } else {
      return null;
    }
  }

  @Nullable
  private static Mob createRandomEntity(Level level, Entity current, TagKey<EntityType<?>> type) {
    EntityType<?> currentType = current.getType();
    EntityType<?> newType =
        getRandomTagEntry(level.getRandom(), BuiltInRegistries.ENTITY_TYPE, type, currentType);
    if (currentType == newType) {
      return null;
    }

    Entity newEntity = newType.create(level, EntitySpawnReason.CONVERSION);
    if (newEntity instanceof Mob) {
      return (Mob) newEntity;
    }

    if (newEntity != null) {
      newEntity.discard();
      PECore.LOGGER.warn(
          "Invalid Entity type {} in mob randomizer tag {}. All entities in this tag are expected"
              + " to be a mob.",
          BuiltInRegistries.ENTITY_TYPE.getKey(newType),
          type.location());
    }

    return null;
  }

  private static EntityType<?> getRandomTagEntry(
      RandomSource random,
      Registry<EntityType<?>> registry,
      TagKey<EntityType<?>> tagKey,
      EntityType<?> toExclude) {
    List<Holder<EntityType<?>>> entries = new ArrayList<>();
    registry.getTagOrEmpty(tagKey).forEach(entries::add);
    if (entries.isEmpty()) {
      return toExclude;
    }

    boolean excludesCurrent = registry.wrapAsHolder(toExclude).is(tagKey);
    if (entries.size() == 1 && excludesCurrent) {
      return toExclude;
    }

    EntityType<?> selected;
    do {
      selected = (EntityType<?>) entries.get(random.nextInt(entries.size())).value();
    } while (selected.equals(toExclude));

    return selected;
  }
}
