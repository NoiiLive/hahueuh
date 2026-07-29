package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.PlayerStat;
import net.noiilive.hahueuh.network.PlayerStatBlock;
import net.noiilive.hahueuh.network.StatEntry;

public final class PlayerStats {
    private PlayerStats() {}

    public static PlayerStatBlock of(ServerPlayer player) {
        return PlayerData.get(player).getStats();
    }

    public static StatEntry get(ServerPlayer player, PlayerStat stat) {
        return of(player).get(stat);
    }

    public static void ensureRolled(ServerPlayer player) {
        if (of(player).rolled()) return;
        reroll(player);
    }

    public static void reroll(ServerPlayer player) {
        int proficiencyMax = ConfigPlayer.STAT_PROFICIENCY_MAX.get();
        int capacityMax = ConfigPlayer.STAT_CAPACITY_MAX.get();
        PlayerStatBlock block = PlayerStatBlock.UNROLLED;
        for (PlayerStat stat : PlayerStat.ORDERED) {
            int proficiency = 1 + player.getRandom().nextInt(proficiencyMax);
            int capacity = 1 + player.getRandom().nextInt(capacityMax);
            block = block.with(stat, new StatEntry(proficiency, capacity, capacity, 0));
        }
        store(player, block);
        StatEffects.refresh(player);
    }

    public static void addProgress(ServerPlayer player, PlayerStat stat, int amount) {
        if (amount <= 0) return;
        ensureRolled(player);

        PlayerStatBlock block = of(player);
        StatEntry entry = block.get(stat);
        int perLevel = Math.max(1, ConfigPlayer.STAT_PROGRESS_PER_LEVEL.get());
        int cap = StatBonuses.levelCap(entry);

        int level = entry.level();
        if (level >= cap) {
            if (entry.progress() >= perLevel) return;
            store(player, block.with(stat, entry.withProgress(perLevel)));
            return;
        }

        int progress = entry.progress() + amount;
        while (progress >= perLevel && level < cap) {
            progress -= perLevel;
            level++;
        }
        if (level >= cap) {
            level = cap;
            progress = perLevel;
        }

        if (level == entry.level() && progress == entry.progress()) return;
        store(player, block.with(stat, entry.withLevelAndProgress(level, progress)));
        if (level != entry.level()) StatEffects.refresh(player);
    }

    public static void addProgress(ServerPlayer player, PlayerStat stat, double amount) {
        addProgress(player, stat, (int) Math.round(amount));
    }

    public static void setLevel(ServerPlayer player, PlayerStat stat, int level) {
        ensureRolled(player);
        PlayerStatBlock block = of(player);
        int cap = StatBonuses.levelCap(block.get(stat));
        store(player, block.with(stat, block.get(stat).withLevelAndProgress(Mth.clamp(level, 0, cap), 0)));
        StatEffects.refresh(player);
    }

    public static int setProficiency(ServerPlayer player, PlayerStat stat, int proficiency) {
        ensureRolled(player);
        PlayerStatBlock block = of(player);
        StatEntry entry = block.get(stat);
        int value = Math.max(1, proficiency);
        store(player, block.with(stat, new StatEntry(value, entry.capacity(), entry.level(), entry.progress())));
        StatEffects.refresh(player);
        return value;
    }

    public static int setCapacity(ServerPlayer player, PlayerStat stat, int capacity) {
        ensureRolled(player);
        PlayerStatBlock block = of(player);
        StatEntry entry = block.get(stat);
        int value = Math.max(1, capacity);
        int cap = value * ConfigPlayer.STAT_CAP_PER_CAPACITY.get();
        int level = Math.min(entry.level(), cap);
        int progress = level >= cap ? Math.min(entry.progress(), ConfigPlayer.STAT_PROGRESS_PER_LEVEL.get())
                : entry.progress();
        store(player, block.with(stat, new StatEntry(entry.proficiency(), value, level, progress)));
        StatEffects.refresh(player);
        return value;
    }

    public static void setProgress(ServerPlayer player, PlayerStat stat, int progress) {
        ensureRolled(player);
        PlayerStatBlock block = of(player);
        int perLevel = Math.max(1, ConfigPlayer.STAT_PROGRESS_PER_LEVEL.get());
        store(player, block.with(stat, block.get(stat).withProgress(Mth.clamp(progress, 0, perLevel))));
    }

    private static void store(ServerPlayer player, PlayerStatBlock block) {
        PlayerData.get(player).setStats(block);
        PlayerDataEvents.sync(player);
    }
}
