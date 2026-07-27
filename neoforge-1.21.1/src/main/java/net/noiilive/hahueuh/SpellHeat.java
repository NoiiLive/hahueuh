package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SpellHeat {
    private static final long DAY_TICKS = 24000L;

    private SpellHeat() {}

    public static int heatFor(int manaPerTick) {
        return Math.max(0, (int) Math.round(manaPerTick * ConfigMagic.SPELL_HEAT_PER_MANA_PER_TICK.get()));
    }

    public static void addHeat(ServerPlayer player, int heat) {
        if (heat <= 0 || player.isCreative()) return;
        ensureCurrentDay(player);

        int maxHeat = BookOfLifeStats.maxMana(player);
        if (maxHeat <= 0) return;

        int current = player.getData(ModAttachments.PLAYER_SPELL_HEAT.get());
        int after = current + heat;
        if (after <= maxHeat) {
            player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), after);
            return;
        }

        int overflow = after - maxHeat;
        player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), maxHeat);
        GateStrain.addStrain(player, overflow);
    }

    public static void clear(ServerPlayer player) {
        player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), 0);
    }

    private static void ensureCurrentDay(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        int currentDay = (int) (server.overworld().getDayTime() / DAY_TICKS);

        int lastDay = player.getData(ModAttachments.PLAYER_HEAT_LAST_RESET_DAY.get());
        if (lastDay < 0) {
            player.setData(ModAttachments.PLAYER_HEAT_LAST_RESET_DAY.get(), currentDay);
            return;
        }

        if (currentDay != lastDay) {
            player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), 0);
            player.setData(ModAttachments.PLAYER_HEAT_LAST_RESET_DAY.get(), currentDay);
        }
    }
}
