package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;

public final class SpellHeat {
    private SpellHeat() {}

    public static int heatFor(int manaPerTick) {
        return Math.max(0, (int) Math.round(manaPerTick * ConfigMagic.SPELL_HEAT_PER_MANA_PER_TICK.get()));
    }

    public static void addHeat(ServerPlayer player, int heat) {
        if (heat <= 0 || player.isCreative()) return;

        PlayerData data = PlayerData.get(player);
        int maxHeat = BookOfLifeStats.maxMana(data);
        if (maxHeat <= 0) return;

        int after = data.getSpellHeat() + heat;
        int capped = Math.min(after, maxHeat);

        data.setSpellHeat(capped);
        data.setHeatDecayBase(capped);
        data.setHeatDecayStart(ResourceDecay.gameTime(player));
        PlayerDataEvents.sync(player);

        if (after > maxHeat) {
            GateStrain.addStrain(player, after - maxHeat);
        }
    }

    public static void clear(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        data.setSpellHeat(0);
        data.setHeatDecayBase(0);
        data.setHeatDecayStart(ResourceDecay.gameTime(player));
        PlayerDataEvents.sync(player);
    }

    public static void tickDecay(MinecraftServer server) {
        int windowSeconds = ConfigMagic.SPELL_HEAT_DECAY_SECONDS.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerData data = PlayerData.get(player);
            int current = data.getSpellHeat();
            if (current <= 0) continue;

            if (data.getHeatDecayBase() <= 0) {
                data.setHeatDecayBase(current);
                data.setHeatDecayStart(ResourceDecay.gameTime(player));
                continue;
            }

            int decayed = ResourceDecay.valueNow(data.getHeatDecayBase(), data.getHeatDecayStart(),
                    ResourceDecay.gameTime(player), windowSeconds, current);
            if (decayed < current) {
                data.setSpellHeat(decayed);
                PlayerDataEvents.sync(player);
            }
        }
    }
}
