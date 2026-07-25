package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.PlayerRace;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerLifespan {
    private PlayerLifespan() {}

    public static void reroll(ServerPlayer player, PlayerRace race) {
        int min = minFor(race);
        int max = maxFor(race);
        if (max < min) { int t = min; min = max; max = t; }
        int roll = min + player.getRandom().nextInt(max - min + 1);
        player.setData(ModAttachments.PLAYER_LIFESPAN.get(), roll);
        BookOfLifeStats.clampToMax(player);
        BookOfLifeStats.setOdToMax(player);
        BookOfLifeAging.checkOldAge(player);
    }

    public static void ensureRolled(ServerPlayer player) {
        if (player.getData(ModAttachments.PLAYER_LIFESPAN.get()) >= 0) return;
        reroll(player, player.getData(ModAttachments.PLAYER_RACE.get()));
    }

    private static int minFor(PlayerRace race) {
        return switch (race) {
            case HUMAN -> ConfigPlayer.HUMAN_LIFESPAN_MIN.getAsInt();
            case ELF -> ConfigPlayer.ELF_LIFESPAN_MIN.getAsInt();
            case HALF_ELF -> ConfigPlayer.HALF_ELF_LIFESPAN_MIN.getAsInt();
        };
    }

    private static int maxFor(PlayerRace race) {
        return switch (race) {
            case HUMAN -> ConfigPlayer.HUMAN_LIFESPAN_MAX.getAsInt();
            case ELF -> ConfigPlayer.ELF_LIFESPAN_MAX.getAsInt();
            case HALF_ELF -> ConfigPlayer.HALF_ELF_LIFESPAN_MAX.getAsInt();
        };
    }
}
