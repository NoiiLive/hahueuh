package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SpellHeat {
    private SpellHeat() {}

    public static int heatFor(int manaPerTick) {
        return Math.max(0, (int) Math.round(manaPerTick * ConfigMagic.SPELL_HEAT_PER_MANA_PER_TICK.get()));
    }

    public static void addHeat(ServerPlayer player, int heat) {
        if (heat <= 0 || player.isCreative()) return;
        if (HahUeuh.LIONS_HEART.isActive(player.getUUID())) return;

        int maxHeat = BookOfLifeStats.maxMana(player);
        if (maxHeat <= 0) return;

        int current = player.getData(ModAttachments.PLAYER_SPELL_HEAT.get());
        int after = current + heat;
        int capped = Math.min(after, maxHeat);

        player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), capped);
        ResourceDecay.restart(player, ModAttachments.PLAYER_HEAT_DECAY_BASE.get(),
                ModAttachments.PLAYER_HEAT_DECAY_START.get(), capped);

        if (after > maxHeat) {
            GateStrain.addStrain(player, after - maxHeat);
        }
    }

    public static void clear(ServerPlayer player) {
        player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), 0);
        ResourceDecay.restart(player, ModAttachments.PLAYER_HEAT_DECAY_BASE.get(),
                ModAttachments.PLAYER_HEAT_DECAY_START.get(), 0);
    }

    public static void tickDecay(MinecraftServer server) {
        int windowSeconds = ConfigMagic.SPELL_HEAT_DECAY_SECONDS.getAsInt();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int current = player.getData(ModAttachments.PLAYER_SPELL_HEAT.get());
            if (current <= 0) continue;
            if (HahUeuh.LIONS_HEART.isActive(player.getUUID())) {
                ResourceDecay.restart(player, ModAttachments.PLAYER_HEAT_DECAY_BASE.get(),
                        ModAttachments.PLAYER_HEAT_DECAY_START.get(), current);
                continue;
            }
            int decayed = ResourceDecay.valueNow(player, ModAttachments.PLAYER_HEAT_DECAY_BASE.get(),
                    ModAttachments.PLAYER_HEAT_DECAY_START.get(), windowSeconds, current);
            if (decayed < current) player.setData(ModAttachments.PLAYER_SPELL_HEAT.get(), decayed);
        }
    }
}
