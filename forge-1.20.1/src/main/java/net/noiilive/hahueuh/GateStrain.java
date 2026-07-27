package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.GateStatus;

public final class GateStrain {
    private GateStrain() {}

    public static void reroll(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        data.setGateOutput(1 + player.getRandom().nextInt(ConfigMagic.GATE_OUTPUT_MAX.get()));
        data.setGateEfficiency(1 + player.getRandom().nextInt(ConfigMagic.GATE_EFFICIENCY_MAX.get()));
        PlayerDataEvents.sync(player);
    }

    public static void ensureRolled(ServerPlayer player) {
        if (PlayerData.get(player).getGateOutput() >= 0) return;
        reroll(player);
    }

    public static void setStrain(ServerPlayer player, int strain) {
        int clamped = Math.max(0, Math.min(100, strain));
        PlayerData.get(player).setGateStrain(clamped);
        applyThresholds(player, clamped);
        PlayerDataEvents.sync(player);
    }

    public static void addStrain(ServerPlayer player, int delta) {
        if (delta > 0 && player.isCreative()) return;
        setStrain(player, PlayerData.get(player).getGateStrain() + delta);
    }

    private static void applyThresholds(ServerPlayer player, int strain) {
        PlayerData data = PlayerData.get(player);
        GateStatus current = data.getGateStatus();
        if (current != GateStatus.OPEN && current != GateStatus.DAMAGED) return;

        int destroyedThreshold = ConfigMagic.GATE_STRAIN_DESTROYED.get();
        int damagedThreshold = ConfigMagic.GATE_STRAIN_DAMAGED.get();

        if (strain >= destroyedThreshold) {
            data.setGateStatus(GateStatus.DESTROYED);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
        } else if (strain >= damagedThreshold && current != GateStatus.DAMAGED) {
            data.setGateStatus(GateStatus.DAMAGED);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}
