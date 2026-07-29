package net.noiilive.hahueuh;

import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.network.PlayerStat;
import net.noiilive.hahueuh.network.StatEntry;

public final class StatBonuses {
    private StatBonuses() {}

    public static StatEntry entry(PlayerData data, PlayerStat stat) {
        return data.getStats().get(stat);
    }

    public static int levelCap(StatEntry entry) {
        return entry.capacity() * ConfigPlayer.STAT_CAP_PER_CAPACITY.get();
    }

    public static boolean atCap(StatEntry entry) {
        return entry.rolled() && entry.level() >= levelCap(entry);
    }

    public static double ratio(StatEntry entry) {
        if (!entry.rolled()) return 0.0;
        int maxProficiency = Math.max(1, ConfigPlayer.STAT_PROFICIENCY_MAX.get());
        int maxCapacity = Math.max(1, ConfigPlayer.STAT_CAPACITY_MAX.get());
        int maxLevel = maxCapacity * Math.max(1, ConfigPlayer.STAT_CAP_PER_CAPACITY.get());
        double denominator = (double) maxProficiency * maxLevel;
        if (denominator <= 0.0) return 0.0;
        return Math.min(1.0, (entry.level() * (double) entry.proficiency()) / denominator);
    }

    public static double ratio(PlayerData data, PlayerStat stat) {
        return ratio(entry(data, stat));
    }

    public static double bonus(PlayerData data, PlayerStat stat, double maxBonus) {
        return ratio(data, stat) * maxBonus;
    }

    public static double multiplier(PlayerData data, PlayerStat stat, double maxBonus) {
        return 1.0 + bonus(data, stat, maxBonus);
    }

    public static double manaCapacityMultiplier(PlayerData data) {
        return multiplier(data, PlayerStat.MAGIC, ConfigPlayer.MAGIC_CAPACITY_BONUS.get());
    }

    public static double manaChargeMultiplier(PlayerData data) {
        return multiplier(data, PlayerStat.MAGIC, ConfigPlayer.MAGIC_CHARGE_BONUS.get());
    }

    public static double gateMultiplier(PlayerData data) {
        return multiplier(data, PlayerStat.MAGIC, ConfigPlayer.MAGIC_GATE_BONUS.get());
    }

    public static int effectiveGateOutput(PlayerData data) {
        int base = Math.max(1, data.getGateOutput());
        return Math.max(1, (int) Math.round(base * gateMultiplier(data)));
    }

    public static int effectiveGateEfficiency(PlayerData data) {
        int base = Math.max(1, data.getGateEfficiency());
        return Math.max(1, (int) Math.round(base * gateMultiplier(data)));
    }
}
