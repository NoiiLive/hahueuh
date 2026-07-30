package net.noiilive.hahueuh;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class InsanityEffect extends MobEffect {
    public InsanityEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            HahUeuh.INSANITY.tickAfflicted(entity, amplifier);
        }
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getCurativeItems() {
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
