package net.noiilive.hahueuh;

import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.server.level.ServerPlayer;

public final class GateDefectiveState {
    private GateDefectiveState() {}

    public static void reroll(ServerPlayer player) {
        GateDefectiveVariant variant = player.getRandom().nextBoolean()
                ? GateDefectiveVariant.NO_ABSORPTION : GateDefectiveVariant.NO_RELEASE;
        PlayerData.get(player).setGateDefectiveVariant(variant.ordinal());
        PlayerDataEvents.sync(player);
    }

    public static void ensureRolled(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data.getGateStatus() != GateStatus.DEFECTIVE) return;
        if (data.getGateDefectiveVariant() >= 0) return;
        reroll(player);
    }
}
