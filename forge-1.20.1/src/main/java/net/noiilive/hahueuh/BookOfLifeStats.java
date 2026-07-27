package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;

public final class BookOfLifeStats {
    public static final int OVERCHARGE_CAP_MULTIPLIER = 2;

    private BookOfLifeStats() {}

    public static int maxOd(PlayerData data) {
        int lifespan = Math.max(0, data.getLifespan());
        return lifespan * ConfigMagic.OD_LIFESPAN_MULTIPLIER.get();
    }

    public static int maxMana(PlayerData data) {
        return maxOd(data);
    }

    public static int maxOd(ServerPlayer player) {
        return maxOd(PlayerData.get(player));
    }

    public static int maxMana(ServerPlayer player) {
        return maxOd(player);
    }

    public static void setOdToMax(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        data.setOdCurrent(maxOd(data));
        PlayerDataEvents.sync(player);
    }

    public static void clampToMax(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        boolean changed = false;

        int maxOd = maxOd(data);
        if (data.getOdCurrent() > maxOd) {
            data.setOdCurrent(maxOd);
            changed = true;
        }

        int overchargeCap = maxMana(data) * OVERCHARGE_CAP_MULTIPLIER;
        if (data.getManaCurrent() > overchargeCap) {
            data.setManaCurrent(overchargeCap);
            changed = true;
        }

        if (changed) PlayerDataEvents.sync(player);
    }
}
