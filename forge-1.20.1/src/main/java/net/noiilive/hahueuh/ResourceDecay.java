package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;

public final class ResourceDecay {
    public static final int TICKS_PER_SECOND = 20;

    private ResourceDecay() {}

    public static int valueNow(int base, long start, long now, int windowSeconds, int fallback) {
        if (base <= 0) return fallback;

        long windowTicks = (long) Math.max(1, windowSeconds) * TICKS_PER_SECOND;
        long elapsed = now - start;
        if (elapsed <= 0) return fallback;
        if (elapsed >= windowTicks) return 0;

        double remaining = 1.0 - (double) elapsed / windowTicks;
        return Math.min(fallback, (int) Math.ceil(base * remaining));
    }

    public static long gameTime(ServerPlayer player) {
        return player.getServer() == null ? 0L : player.getServer().overworld().getGameTime();
    }
}
