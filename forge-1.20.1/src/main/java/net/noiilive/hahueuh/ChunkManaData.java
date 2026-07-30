package net.noiilive.hahueuh;

import net.minecraft.world.level.chunk.LevelChunk;
import net.noiilive.hahueuh.capability.ChunkMana;
import net.noiilive.hahueuh.capability.ModCapabilities;

public final class ChunkManaData {
    private static final long TICKS_PER_DAY = 24000L;

    private ChunkManaData() {}

    private static double ratePerTick() {
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.get();
        double days = ConfigMagic.CHUNK_REPLENISH_DAYS.get();
        return cap / (days * TICKS_PER_DAY);
    }

    public static int available(LevelChunk chunk) {
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.get();
        return (int) Math.floor(currentAmount(chunk, cap));
    }

    public static int drain(LevelChunk chunk, int requested) {
        if (requested <= 0) return 0;
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.get();
        double current = currentAmount(chunk, cap);
        int drained = Math.min(requested, (int) Math.floor(current));
        if (drained <= 0) return 0;
        store(chunk, current - drained);
        return drained;
    }

    public static void set(LevelChunk chunk, int amount) {
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.get();
        store(chunk, Math.max(0, Math.min(cap, amount)));
    }

    private static double currentAmount(LevelChunk chunk, int cap) {
        ChunkMana data = chunk.getCapability(ModCapabilities.CHUNK_MANA).orElse(null);
        if (data == null || data.uninitialized()) return cap;
        long now = chunk.getLevel().getGameTime();
        long elapsed = now - data.lastTick();
        if (elapsed < 0L) {
            store(chunk, data.amount());
            return Math.min(cap, Math.max(0, data.amount()));
        }
        return Math.min(cap, Math.max(0, data.amount() + elapsed * ratePerTick()));
    }

    private static void store(LevelChunk chunk, double amount) {
        chunk.getCapability(ModCapabilities.CHUNK_MANA).ifPresent(data -> {
            data.set(amount, chunk.getLevel().getGameTime());
            chunk.setUnsaved(true);
        });
    }
}
