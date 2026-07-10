package net.noiilive.hahueuh;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HahUeuh.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FrozenObjectProjectile>> FROZEN_OBJECT_PROJECTILE =
            ENTITY_TYPES.register("frozen_object_projectile", () -> EntityType.Builder
                    .<FrozenObjectProjectile>of(FrozenObjectProjectile::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(6)
                    .updateInterval(5)
                    .noSave()
                    .build("frozen_object_projectile"));
}
