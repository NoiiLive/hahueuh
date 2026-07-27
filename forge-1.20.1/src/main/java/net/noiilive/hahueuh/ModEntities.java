package net.noiilive.hahueuh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HahUeuh.MODID);

    public static final RegistryObject<EntityType<FrozenObjectProjectile>> FROZEN_OBJECT_PROJECTILE =
            ENTITY_TYPES.register("frozen_object_projectile", () -> EntityType.Builder
                    .<FrozenObjectProjectile>of(FrozenObjectProjectile::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(6)
                    .updateInterval(5)
                    .noSave()
                    .build("frozen_object_projectile"));

    public static final RegistryObject<EntityType<MinyaSpikeEntity>> MINYA_SPIKE =
            ENTITY_TYPES.register("minya_spike", () -> EntityType.Builder
                    .<MinyaSpikeEntity>of(MinyaSpikeEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build("minya_spike"));

    public static final RegistryObject<EntityType<MinyaRingEntity>> MINYA_RING =
            ENTITY_TYPES.register("minya_ring", () -> EntityType.Builder
                    .<MinyaRingEntity>of(MinyaRingEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.1f)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .noSave()
                    .fireImmune()
                    .build("minya_ring"));

    public static final RegistryObject<EntityType<BlackHoleEntity>> BLACK_HOLE =
            ENTITY_TYPES.register("black_hole", () -> EntityType.Builder
                    .<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .noSave()
                    .build("black_hole"));

    public static final RegistryObject<EntityType<WitchFactorEntity>> WITCH_FACTOR =
            ENTITY_TYPES.register("witch_factor", () -> EntityType.Builder
                    .<WitchFactorEntity>of(WitchFactorEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .build("witch_factor"));

    private ModEntities() {}
}
