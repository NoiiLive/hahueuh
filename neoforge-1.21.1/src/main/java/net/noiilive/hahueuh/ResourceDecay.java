package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class ResourceDecay {
    public static final int TICKS_PER_SECOND = 20;

    private ResourceDecay() {}

    public static void restart(ServerPlayer player, AttachmentType<Integer> baseType,
                               AttachmentType<Long> startType, int value) {
        player.setData(baseType, Math.max(0, value));
        player.setData(startType, gameTime(player));
    }

    public static int valueNow(ServerPlayer player, AttachmentType<Integer> baseType,
                               AttachmentType<Long> startType, int windowSeconds, int fallback) {
        int base = player.getData(baseType);
        if (base <= 0) {
            restart(player, baseType, startType, fallback);
            return fallback;
        }

        long windowTicks = (long) Math.max(1, windowSeconds) * TICKS_PER_SECOND;
        long now = gameTime(player);
        long elapsed = now - player.getData(startType);
        if (elapsed < 0) {
            player.setData(startType, now);
            return fallback;
        }
        if (elapsed == 0) return fallback;
        if (elapsed >= windowTicks) return 0;

        double remaining = 1.0 - (double) elapsed / windowTicks;
        return Math.min(fallback, (int) Math.ceil(base * remaining));
    }

    private static long gameTime(ServerPlayer player) {
        return player.getServer() == null ? 0L : player.getServer().overworld().getGameTime();
    }
}
