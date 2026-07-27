package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManaCharging {
    private static final int TICKS_PER_SECOND = 20;

    private final Set<UUID> charging = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> chargeAccumulator = new ConcurrentHashMap<>();

    public void setCharging(ServerPlayer player, boolean active) {
        if (active) {
            charging.add(player.getUUID());
        } else {
            charging.remove(player.getUUID());
            chargeAccumulator.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        charging.remove(event.getEntity().getUUID());
        chargeAccumulator.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        boolean secondTick = server.getTickCount() % TICKS_PER_SECOND == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (secondTick) applySustainedOverchargeStrain(player);

            if (defectiveVariantOf(player) == GateDefectiveVariant.NO_ABSORPTION) continue;
            if (!charging.contains(player.getUUID())) continue;
            chargeManaTick(player);
        }
    }

    private static void applySustainedOverchargeStrain(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        int max = BookOfLifeStats.maxMana(data);
        if (max <= 0) return;
        int overcharge = data.getManaCurrent() - max;
        if (overcharge <= 0) return;
        int freeTiers = ConfigMagic.OVERCHARGE_FREE_HEADROOM_PERCENT.get() / 10;
        int strainTiers = overchargeTier(overcharge, max) - freeTiers;
        if (strainTiers <= 0) return;
        GateStrain.addStrain(player, strainTiers * ConfigMagic.OVERCHARGE_STRAIN_PER_TIER_PER_SECOND.get());
    }

    private static GateDefectiveVariant defectiveVariantOf(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data.getGateStatus() != GateStatus.DEFECTIVE) return null;
        return GateDefectiveVariant.byOrdinal(data.getGateDefectiveVariant());
    }

    private void chargeManaTick(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        int max = BookOfLifeStats.maxMana(data);
        if (max <= 0) return;
        int overchargeCap = max * BookOfLifeStats.OVERCHARGE_CAP_MULTIPLIER;
        int current = data.getManaCurrent();
        int room = overchargeCap - current;
        if (room <= 0) return;

        double perSecond = max * ConfigMagic.MANA_CHARGE_PERCENT_PER_SECOND.get() / 100.0;
        double perTick = Math.max(perSecond, 1.0) / TICKS_PER_SECOND;
        double accumulated = chargeAccumulator.merge(player.getUUID(), perTick, Double::sum);

        int want = Math.min((int) accumulated, room);
        if (want <= 0) return;

        LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
        int drawn = ChunkManaData.drain(chunk, want);
        chargeAccumulator.merge(player.getUUID(), (double) -want, Double::sum);
        if (drawn <= 0) {
            player.displayClientMessage(Component.translatable("hahueuh.message.chunk_mana_depleted")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
            return;
        }

        data.setManaCurrent(current + drawn);
        PlayerDataEvents.sync(player);

        applyMiasmaExposure(player, chunk);
    }

    private static void applyMiasmaExposure(ServerPlayer player, LevelChunk chunk) {
        if (player.isCreative() || player.isSpectator()) return;
        int threshold = Miasma.effectThreshold();
        int miasma = ChunkMiasmaData.get(chunk);
        if (miasma >= threshold) {
            Miasma.applySickness(player, miasma - threshold);
        }
    }

    private static int overchargeTier(int overcharge, int max) {
        return (overcharge * 10) / max;
    }
}
