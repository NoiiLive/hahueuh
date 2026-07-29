package net.noiilive.hahueuh;

import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.noiilive.hahueuh.network.PlayerStat;
import net.noiilive.hahueuh.network.PlayerStatBlock;
import net.noiilive.hahueuh.network.StatEntry;

public final class StatBonuses {
    private StatBonuses() {}

    public static PlayerStatBlock blockOf(IAttachmentHolder holder) {
        return holder.getData(ModAttachments.PLAYER_STATS.get());
    }

    public static StatEntry entry(IAttachmentHolder holder, PlayerStat stat) {
        return blockOf(holder).get(stat);
    }

    public static int levelCap(StatEntry entry) {
        return entry.capacity() * ConfigPlayer.STAT_CAP_PER_CAPACITY.getAsInt();
    }

    public static boolean atCap(StatEntry entry) {
        return entry.rolled() && entry.level() >= levelCap(entry);
    }

    public static double ratio(StatEntry entry) {
        if (!entry.rolled()) return 0.0;
        int maxProficiency = Math.max(1, ConfigPlayer.STAT_PROFICIENCY_MAX.getAsInt());
        int maxCapacity = Math.max(1, ConfigPlayer.STAT_CAPACITY_MAX.getAsInt());
        int maxLevel = maxCapacity * Math.max(1, ConfigPlayer.STAT_CAP_PER_CAPACITY.getAsInt());
        double denominator = (double) maxProficiency * maxLevel;
        if (denominator <= 0.0) return 0.0;
        return Math.min(1.0, (entry.level() * (double) entry.proficiency()) / denominator);
    }

    public static double ratio(IAttachmentHolder holder, PlayerStat stat) {
        return ratio(entry(holder, stat));
    }

    public static double bonus(IAttachmentHolder holder, PlayerStat stat, double maxBonus) {
        return ratio(holder, stat) * maxBonus;
    }

    public static double multiplier(IAttachmentHolder holder, PlayerStat stat, double maxBonus) {
        return 1.0 + bonus(holder, stat, maxBonus);
    }

    public static double manaCapacityMultiplier(IAttachmentHolder holder) {
        return multiplier(holder, PlayerStat.MAGIC, ConfigPlayer.MAGIC_CAPACITY_BONUS.get());
    }

    public static double manaChargeMultiplier(IAttachmentHolder holder) {
        return multiplier(holder, PlayerStat.MAGIC, ConfigPlayer.MAGIC_CHARGE_BONUS.get());
    }

    public static double gateMultiplier(IAttachmentHolder holder) {
        return multiplier(holder, PlayerStat.MAGIC, ConfigPlayer.MAGIC_GATE_BONUS.get());
    }

    public static int effectiveGateOutput(IAttachmentHolder holder) {
        int base = Math.max(1, holder.getData(ModAttachments.PLAYER_GATE_OUTPUT.get()));
        return Math.max(1, (int) Math.round(base * gateMultiplier(holder)));
    }

    public static int effectiveGateEfficiency(IAttachmentHolder holder) {
        int base = Math.max(1, holder.getData(ModAttachments.PLAYER_GATE_EFFICIENCY.get()));
        return Math.max(1, (int) Math.round(base * gateMultiplier(holder)));
    }
}
