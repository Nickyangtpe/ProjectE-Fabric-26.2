package moze_intel.projecte.gameObjs.registration.impl;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import moze_intel.projecte.gameObjs.entity.EntityNovaPrimed;
import moze_intel.projecte.gameObjs.entity.NoGravityThrowableProjectile;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;

public class EntityTypeDeferredRegister extends PEDeferredRegister<EntityType<?>> {
  public EntityTypeDeferredRegister(String modid) {
    super(Registries.ENTITY_TYPE, modid, EntityTypeRegistryObject::new);
  }

  public <ENTITY extends Entity> EntityTypeRegistryObject<ENTITY> registerBuilder(
      String name, Supplier<Builder<ENTITY>> builder, UnaryOperator<Builder<ENTITY>> modifier) {
    return this.registerEntityType(
        name,
        rl -> modifier.apply(builder.get()).build(ResourceKey.create(Registries.ENTITY_TYPE, rl)));
  }

  private <ENTITY extends Entity> EntityTypeRegistryObject<ENTITY> registerEntityType(
      String name, Function<Identifier, EntityType<ENTITY>> factory) {
    return (EntityTypeRegistryObject<ENTITY>) super.<EntityType<ENTITY>>register(name, factory);
  }

  public <ENTITY extends Entity> EntityTypeRegistryObject<ENTITY> registerMisc(
      String name, EntityFactory<ENTITY> factory, UnaryOperator<Builder<ENTITY>> modifier) {
    return this.registerBuilder(name, () -> Builder.of(factory, MobCategory.MISC), modifier);
  }

  public <ENTITY extends NoGravityThrowableProjectile>
      EntityTypeRegistryObject<ENTITY> registerNoGravThrowable(
          String name, EntityFactory<ENTITY> factory) {
    return this.registerMisc(
        name,
        factory,
        builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(10));
  }

  public <ENTITY extends EntityNovaPrimed> EntityTypeRegistryObject<ENTITY> registerTnt(
      String name, EntityFactory<ENTITY> factory) {
    return this.registerMisc(
        name,
        factory,
        builder ->
            builder
                .fireImmune()
                .sized(0.98F, 0.98F)
                .eyeHeight(0.15F)
                .clientTrackingRange(10)
                .updateInterval(10));
  }
}
