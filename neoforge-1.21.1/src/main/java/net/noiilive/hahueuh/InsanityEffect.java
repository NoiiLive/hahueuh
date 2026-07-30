package net.noiilive.hahueuh;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class InsanityEffect extends MobEffect {
    public InsanityEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            HahUeuh.INSANITY.tickAfflicted(entity, amplifier);
        }
        return true;
    }

    @Override
    public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, net.minecraft.world.effect.MobEffectInstance effectInstance) {
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
