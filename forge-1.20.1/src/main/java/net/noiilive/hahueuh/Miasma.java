package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class Miasma {

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

}
