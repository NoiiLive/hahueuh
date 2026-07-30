package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        boolean secondTick = server.getTickCount() % TICKS_PER_SECOND == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (secondTick) applySustainedOverchargeStrain(player);

            if (defectiveVariantOf(player) == GateDefectiveVariant.NO_ABSORPTION) continue;
            if (!charging.contains(player.getUUID())) continue;
            if (player.getData(ModAttachments.PLAYER_GATE_STATUS.get()) == GateStatus.DESTROYED) {
                if (secondTick) {
                    player.displayClientMessage(Component.translatable("hahueuh.message.spell_gate_destroyed")
                            .withStyle(ChatFormatting.DARK_GRAY), true);
                }
                continue;
            }
            if (HahUeuh.LIONS_HEART.isActive(player.getUUID())) {
                if (secondTick) {
                    player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_frozen_gate")
                            .withStyle(ChatFormatting.DARK_GRAY), true);
                }
                continue;
            }
            if (HahUeuh.EMT.suppresses(player)) {
                if (secondTick) {
                    player.displayClientMessage(Component.translatable("hahueuh.message.emt_silenced")
                            .withStyle(ChatFormatting.DARK_GRAY), true);
                }
                continue;
            }
            chargeManaTick(player);
        }
    }

    private static void applySustainedOverchargeStrain(ServerPlayer player) {
        int max = BookOfLifeStats.maxMana(player);
        if (max <= 0) return;
        int overcharge = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get()) - max;
        if (overcharge <= 0) return;
        int freeTiers = ConfigMagic.OVERCHARGE_FREE_HEADROOM_PERCENT.getAsInt() / 10;
        int strainTiers = overchargeTier(overcharge, max) - freeTiers;
        if (strainTiers <= 0) return;
        GateStrain.addStrain(player, strainTiers * ConfigMagic.OVERCHARGE_STRAIN_PER_TIER_PER_SECOND.getAsInt());
    }

    private static GateDefectiveVariant defectiveVariantOf(ServerPlayer player) {
        if (player.getData(ModAttachments.PLAYER_GATE_STATUS.get()) != GateStatus.DEFECTIVE) return null;
        return GateDefectiveVariant.byOrdinal(player.getData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get()));
    }


    private void chargeManaTick(ServerPlayer player) {
        int max = BookOfLifeStats.maxMana(player);
        if (max <= 0) return;
        int overchargeCap = max * BookOfLifeStats.OVERCHARGE_CAP_MULTIPLIER;
        int current = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        int room = overchargeCap - current;
        if (room <= 0) return;

        double perSecond = max * ConfigMagic.MANA_CHARGE_PERCENT_PER_SECOND.get() / 100.0
                * StatBonuses.manaChargeMultiplier(player);
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

        player.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), current + drawn);
        HahUeuh.STAT_EFFECTS.awardManaCharged(player, drawn);

        applyMiasmaExposure(player, chunk);

    }

    private static void applyMiasmaExposure(ServerPlayer player, LevelChunk chunk) {
        if (player.isCreative() || player.isSpectator()) return;
        if (HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().hasAnyWitchFactor(player.getUUID())) return;

        int threshold = Miasma.effectThreshold();
        int miasma = ChunkMiasmaData.get(chunk);
        if (miasma >= threshold) {
            HahUeuh.INSANITY.addDrawExposure(player, miasma - threshold);
        }
    }

    private static int overchargeTier(int overcharge, int max) {
        return (overcharge * 10) / max;
    }
}
