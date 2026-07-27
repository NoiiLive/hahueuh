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
        int current = player.getData(ModAttachments.PLAYER_GATE_STRAIN.get());
        setStrain(player, current + delta);
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
