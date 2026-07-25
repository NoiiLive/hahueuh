package net.noiilive.hahueuh.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AttackStrengthTickerAccessor {
    @Accessor("attackStrengthTicker")
    void hahueuh$setAttackStrengthTicker(int value);
}
