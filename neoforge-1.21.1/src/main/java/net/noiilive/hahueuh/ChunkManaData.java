package net.noiilive.hahueuh;

import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkManaData {
    private static final long TICKS_PER_DAY = 24000L;

    private ChunkManaData() {}

    private static double ratePerTick() {
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.getAsInt();
        double days = ConfigMagic.CHUNK_REPLENISH_DAYS.get();
        return cap / (days * TICKS_PER_DAY);
    }

    public static int available(LevelChunk chunk) {
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.getAsInt();
        return (int) Math.floor(currentAmount(chunk, cap));
    }

    public static int drain(LevelChunk chunk, int requested) {
        if (requested <= 0) return 0;
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.getAsInt();
        double current = currentAmount(chunk, cap);
        int drained = Math.min(requested, (int) Math.floor(current));
        if (drained <= 0) return 0;
        store(chunk, current - drained);
        return drained;
    }

    public static void set(LevelChunk chunk, int amount) {
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.getAsInt();
        store(chunk, Math.max(0, Math.min(cap, amount)));
    }

    private static double currentAmount(LevelChunk chunk, int cap) {
        ChunkMana data = chunk.hasData(ModAttachments.CHUNK_AMBIENT_MANA.get())
                ? chunk.getData(ModAttachments.CHUNK_AMBIENT_MANA.get()) : null;
        if (data == null || data.uninitialized()) return cap;
        long now = chunk.getLevel().getGameTime();
        long elapsed = now - data.lastTick();
        double amount = elapsed <= 0 ? data.amount() : data.amount() + elapsed * ratePerTick();
        return Math.min(cap, Math.max(0, amount));
    }

    private static void store(LevelChunk chunk, double amount) {
        chunk.setData(ModAttachments.CHUNK_AMBIENT_MANA.get(),
                new ChunkMana(amount, chunk.getLevel().getGameTime()));
    }
}
