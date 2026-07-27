package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.noiilive.hahueuh.DualWield;
import net.noiilive.hahueuh.network.ClientDualWieldState;

public final class DualWieldClientHandler {
    private DualWieldClientHandler() {}

    public static void applyAttackStrength(boolean offhandNext, float scale) {
        ClientDualWieldState.setOffhandNext(offhandNext);
        if (Minecraft.getInstance().player != null) {
            DualWield.applyIndicator(Minecraft.getInstance().player, scale);
        }
    }
}
