package net.noiilive.hahueuh;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> MINYA =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(HahUeuh.MODID, "minya"));

    private ModDamageTypes() {}

    public static DamageSource minya(Level level, Entity direct, Entity causer) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MINYA),
                direct, causer);
    }
}
