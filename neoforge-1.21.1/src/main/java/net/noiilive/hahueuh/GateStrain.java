package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class GateStrain {
    private GateStrain() {}

    public static void reroll(ServerPlayer player) {
        player.setData(ModAttachments.PLAYER_GATE_OUTPUT.get(),
                1 + player.getRandom().nextInt(ConfigMagic.GATE_OUTPUT_MAX.getAsInt()));
        player.setData(ModAttachments.PLAYER_GATE_EFFICIENCY.get(),
                1 + player.getRandom().nextInt(ConfigMagic.GATE_EFFICIENCY_MAX.getAsInt()));
    }

    public static void ensureRolled(ServerPlayer player) {
        if (player.getData(ModAttachments.PLAYER_GATE_OUTPUT.get()) >= 0) return;
        reroll(player);
    }

    public static void setStrain(ServerPlayer player, int strain) {
        int clamped = Math.max(0, Math.min(100, strain));
        player.setData(ModAttachments.PLAYER_GATE_STRAIN.get(), clamped);
        applyThresholds(player, clamped);
    }

    public static void addStrain(ServerPlayer player, int delta) {
        if (delta > 0 && player.isCreative()) return;
        if (HahUeuh.LIONS_HEART.isActive(player.getUUID())) return;
        int current = player.getData(ModAttachments.PLAYER_GATE_STRAIN.get());
        setStrain(player, current + delta);
        if (delta > 0) {
            ResourceDecay.restart(player, ModAttachments.PLAYER_STRAIN_DECAY_BASE.get(),
                    ModAttachments.PLAYER_STRAIN_DECAY_START.get(),
                    player.getData(ModAttachments.PLAYER_GATE_STRAIN.get()));
        }
    }

    public static void tickDecay(net.minecraft.server.MinecraftServer server) {
        int windowSeconds = ConfigMagic.GATE_STRAIN_DECAY_SECONDS.getAsInt();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int current = player.getData(ModAttachments.PLAYER_GATE_STRAIN.get());
            if (current <= 0) continue;
            if (HahUeuh.LIONS_HEART.isActive(player.getUUID())) {
                ResourceDecay.restart(player, ModAttachments.PLAYER_STRAIN_DECAY_BASE.get(),
                        ModAttachments.PLAYER_STRAIN_DECAY_START.get(), current);
                continue;
            }
            int decayed = ResourceDecay.valueNow(player, ModAttachments.PLAYER_STRAIN_DECAY_BASE.get(),
                    ModAttachments.PLAYER_STRAIN_DECAY_START.get(), windowSeconds, current);
            if (decayed < current) {
                player.setData(ModAttachments.PLAYER_GATE_STRAIN.get(), decayed);
            }
        }
    }

    private static void applyThresholds(ServerPlayer player, int strain) {
        GateStatus current = player.getData(ModAttachments.PLAYER_GATE_STATUS.get());
        if (current != GateStatus.OPEN && current != GateStatus.DAMAGED) return;

        int destroyedThreshold = ConfigMagic.GATE_STRAIN_DESTROYED.getAsInt();
        int damagedThreshold = ConfigMagic.GATE_STRAIN_DAMAGED.getAsInt();

        if (strain >= destroyedThreshold) {
            player.setData(ModAttachments.PLAYER_GATE_STATUS.get(), GateStatus.DESTROYED);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
        } else if (strain >= damagedThreshold && current != GateStatus.DAMAGED) {
            player.setData(ModAttachments.PLAYER_GATE_STATUS.get(), GateStatus.DAMAGED);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}
