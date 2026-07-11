package net.noiilive.hahueuh.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record WorldSnapshot(
        Path checkpointDir,
        Map<ResourceKey<Level>, List<CompoundTag>> entityData,
        Map<ResourceKey<Level>, Set<Long>> loadedChunks,
        Map<UUID, PlayerSnapshot> playerData,
        long gameTime,
        long dayTime,
        boolean raining,
        boolean thundering,
        int clearWeatherTime,
        int rainTime,
        int thunderTime,
        Map<Path, Long> fileTimestamps,
        Map<UUID, Integer> domainCooldownRemaining,
        Map<UUID, Integer> lionsHeartCooldownRemaining,
        Map<UUID, Integer> littleKingCooldownRemaining,
        Map<UUID, Integer> materialPhaseCooldownRemaining,
        Map<UUID, Integer> objectFreezeCooldownRemaining,
        Map<UUID, Integer> allyTrackerCooldownRemaining,
        Map<UUID, Integer> baseShiftCooldownRemaining,
        Map<UUID, Integer> secondShiftCooldownRemaining
) {
}
