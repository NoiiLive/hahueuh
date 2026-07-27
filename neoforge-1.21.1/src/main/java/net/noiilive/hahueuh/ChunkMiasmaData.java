package net.noiilive.hahueuh;

import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkMiasmaData {
    private static final long TICKS_PER_DAY = 24000L;

    private ChunkMiasmaData() {}

    private static double decayPerTick() {
        int cap = ConfigMagic.MIASMA_CAP.getAsInt();
        double days = ConfigMagic.MIASMA_DECAY_DAYS.get();
        return cap / (days * TICKS_PER_DAY);
    }

    public static int get(LevelChunk chunk) {
        return (int) Math.floor(currentAmount(chunk));
    }

    public static void add(LevelChunk chunk, int delta) {
        if (delta <= 0) return;
        int cap = ConfigMagic.MIASMA_CAP.getAsInt();
        double next = Math.min(cap, currentAmount(chunk) + delta);
        store(chunk, next);
    }

    public static void set(LevelChunk chunk, int amount) {
        int cap = ConfigMagic.MIASMA_CAP.getAsInt();
        store(chunk, Math.max(0, Math.min(cap, amount)));
    }

    private static double currentAmount(LevelChunk chunk) {
        ChunkMiasma data = chunk.hasData(ModAttachments.CHUNK_MIASMA.get())
                ? chunk.getData(ModAttachments.CHUNK_MIASMA.get()) : null;
        if (data == null || data.uninitialized()) return 0.0;
        long now = chunk.getLevel().getGameTime();
        long elapsed = now - data.lastTick();
        double amount = elapsed <= 0 ? data.amount() : data.amount() - elapsed * decayPerTick();
        return Math.max(0.0, amount);
    }

    private static void store(LevelChunk chunk, double amount) {
        chunk.setData(ModAttachments.CHUNK_MIASMA.get(),
                new ChunkMiasma(amount, chunk.getLevel().getGameTime()));
    }
}
