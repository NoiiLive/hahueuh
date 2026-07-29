package net.noiilive.hahueuh.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LastDamageAccessor {
    @Accessor("lastDamageSource")
    DamageSource hahueuh$getLastDamageSource();

    @Accessor("lastDamageSource")
    void hahueuh$setLastDamageSource(DamageSource value);

    @Accessor("lastDamageStamp")
    long hahueuh$getLastDamageStamp();

    @Accessor("lastDamageStamp")
    void hahueuh$setLastDamageStamp(long value);
}
