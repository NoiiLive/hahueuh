package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.server.level.ServerPlayer;

public final class GateDefectiveState {
    private GateDefectiveState() {}

    public static void reroll(ServerPlayer player) {
        GateDefectiveVariant variant = player.getRandom().nextBoolean()
                ? GateDefectiveVariant.NO_ABSORPTION : GateDefectiveVariant.NO_RELEASE;
        player.setData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get(), variant.ordinal());
    }

    public static void ensureRolled(ServerPlayer player) {
        if (player.getData(ModAttachments.PLAYER_GATE_STATUS.get()) != GateStatus.DEFECTIVE) return;
        if (player.getData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get()) >= 0) return;
        reroll(player);
    }
}
