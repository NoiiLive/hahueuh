package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.PlayerRace;

public final class PlayerLifespan {
    private PlayerLifespan() {}

    public static void reroll(ServerPlayer player, PlayerRace race) {
        int min = minFor(race);
        int max = maxFor(race);
        if (max < min) { int t = min; min = max; max = t; }
        int roll = min + player.getRandom().nextInt(max - min + 1);
        PlayerData.get(player).setLifespan(roll);
        PlayerDataEvents.sync(player);
        BookOfLifeStats.clampToMax(player);
        BookOfLifeStats.setOdToMax(player);
        BookOfLifeAging.checkOldAge(player);
    }

    public static void ensureRolled(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data.getLifespan() >= 0) return;
        reroll(player, data.getRace());
    }

    private static int minFor(PlayerRace race) {
        return switch (race) {
            case HUMAN -> ConfigPlayer.HUMAN_LIFESPAN_MIN.get();
            case ELF -> ConfigPlayer.ELF_LIFESPAN_MIN.get();
            case HALF_ELF -> ConfigPlayer.HALF_ELF_LIFESPAN_MIN.get();
        };
    }

    private static int maxFor(PlayerRace race) {
        return switch (race) {
            case HUMAN -> ConfigPlayer.HUMAN_LIFESPAN_MAX.get();
            case ELF -> ConfigPlayer.ELF_LIFESPAN_MAX.get();
            case HALF_ELF -> ConfigPlayer.HALF_ELF_LIFESPAN_MAX.get();
        };
    }
}
