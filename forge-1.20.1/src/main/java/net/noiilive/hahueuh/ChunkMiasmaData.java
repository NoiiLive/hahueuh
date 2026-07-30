package net.noiilive.hahueuh;

import net.minecraft.world.level.chunk.LevelChunk;
import net.noiilive.hahueuh.capability.ChunkMiasma;
import net.noiilive.hahueuh.capability.ModCapabilities;

public final class ChunkMiasmaData {
    private static final long TICKS_PER_DAY = 24000L;

    private ChunkMiasmaData() {}

    private static double decayPerTick() {
        int cap = ConfigMagic.MIASMA_CAP.get();
        double days = ConfigMagic.MIASMA_DECAY_DAYS.get();
        return cap / (days * TICKS_PER_DAY);
    }

    public static int get(LevelChunk chunk) {
        return (int) Math.floor(currentAmount(chunk));
    }

    public static void add(LevelChunk chunk, int delta) {
        if (delta <= 0) return;
        int cap = ConfigMagic.MIASMA_CAP.get();
        double next = Math.min(cap, currentAmount(chunk) + delta);
        store(chunk, next);
    }

    public static void set(LevelChunk chunk, int amount) {
        int cap = ConfigMagic.MIASMA_CAP.get();
        store(chunk, Math.max(0, Math.min(cap, amount)));
    }

    private static double currentAmount(LevelChunk chunk) {
        ChunkMiasma data = chunk.getCapability(ModCapabilities.CHUNK_MIASMA).orElse(null);
        if (data == null || data.uninitialized()) return 0.0;
        long now = chunk.getLevel().getGameTime();
        long elapsed = now - data.lastTick();
        if (elapsed < 0L) {
            store(chunk, data.amount());
            return Math.max(0.0, data.amount());
        }
        return Math.max(0.0, data.amount() - elapsed * decayPerTick());
    }

    private static void store(LevelChunk chunk, double amount) {
        chunk.getCapability(ModCapabilities.CHUNK_MIASMA).ifPresent(data -> {
            data.set(amount, chunk.getLevel().getGameTime());
            chunk.setUnsaved(true);
        });
    }
}
