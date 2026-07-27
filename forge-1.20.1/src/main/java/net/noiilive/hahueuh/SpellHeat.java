package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;

public final class SpellHeat {
    private static final long DAY_TICKS = 24000L;

    private SpellHeat() {}

    public static int heatFor(int manaPerTick) {
        return Math.max(0, (int) Math.round(manaPerTick * ConfigMagic.SPELL_HEAT_PER_MANA_PER_TICK.get()));
    }

    public static void addHeat(ServerPlayer player, int heat) {
        if (heat <= 0 || player.isCreative()) return;
        ensureCurrentDay(player);

        PlayerData data = PlayerData.get(player);
        int maxHeat = BookOfLifeStats.maxMana(data);
        if (maxHeat <= 0) return;

        int after = data.getSpellHeat() + heat;
        if (after <= maxHeat) {
            data.setSpellHeat(after);
            PlayerDataEvents.sync(player);
            return;
        }

        int overflow = after - maxHeat;
        data.setSpellHeat(maxHeat);
        GateStrain.addStrain(player, overflow);
    }

    public static void clear(ServerPlayer player) {
        PlayerData.get(player).setSpellHeat(0);
        PlayerDataEvents.sync(player);
    }

    private static void ensureCurrentDay(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        int currentDay = (int) (server.overworld().getDayTime() / DAY_TICKS);

        PlayerData data = PlayerData.get(player);
        int lastDay = data.getHeatLastResetDay();
        if (lastDay < 0) {
            data.setHeatLastResetDay(currentDay);
            return;
        }

        if (currentDay != lastDay) {
            data.setSpellHeat(0);
            data.setHeatLastResetDay(currentDay);
        }
    }
}
