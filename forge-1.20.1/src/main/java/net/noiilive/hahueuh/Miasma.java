package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class Miasma {
    private static final int EFFECT_DURATION_TICKS = 40;

    private Miasma() {}

    public static int effectThreshold() {
        return (int) ((long) ConfigMagic.MIASMA_CAP.get() * ConfigMagic.MIASMA_EFFECT_THRESHOLD_PERCENT.get() / 100L);
    }

    public static void addSingleUse(LivingEntity user) {
        LevelChunk chunk = chunkOf(user);
        if (chunk != null) ChunkMiasmaData.add(chunk, ConfigMagic.MIASMA_PER_SINGLE_USE.get());
    }

    public static void addToggleSecond(LivingEntity user) {
        LevelChunk chunk = chunkOf(user);
        if (chunk != null) ChunkMiasmaData.add(chunk, ConfigMagic.MIASMA_PER_TOGGLE_SECOND.get());
    }

    public static boolean hasActiveSinToggle(LivingEntity entity) {
        java.util.UUID id = entity.getUUID();
        return HahUeuh.LIONS_HEART.isActive(id)
                || HahUeuh.MATERIAL_PHASE.isActive(id)
                || HahUeuh.BASE_SHIFT.isActive(id)
                || HahUeuh.SECOND_SHIFT.isActive(id)
                || HahUeuh.BOOK_OF_WISDOM.isSummoned(id)
                || HahUeuh.VISION_OF_DANGER.isActive(id)
                || HahUeuh.VISION_OF_LIFE.isActive(id)
                || HahUeuh.SNAPSHOT_MANAGER.hasSustainedUnseenHand(id);
    }

    public static LevelChunk chunkOf(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return null;
        return level.getChunkAt(entity.blockPosition());
    }

    public static void applySickness(LivingEntity entity, int severity) {
        int range = Math.max(1, ConfigMagic.MIASMA_CAP.get() - effectThreshold());

        addEffect(entity, MobEffects.CONFUSION, 0);
        addEffect(entity, MobEffects.WEAKNESS, severity / 20);
        addEffect(entity, MobEffects.HUNGER, severity / 20);
        if (severity >= range * 0.6) addEffect(entity, MobEffects.POISON, 0);
        if (severity >= range * 0.9) addEffect(entity, MobEffects.WITHER, 0);
    }

    private static void addEffect(LivingEntity entity, MobEffect effect, int amplifier) {
        entity.forceAddEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS,
                Math.max(0, amplifier), false, false, true), null);
    }
}
