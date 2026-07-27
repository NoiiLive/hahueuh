package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.noiilive.hahueuh.MagicSchool;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.network.ClientPlayerData;

public final class ClientMagicState {
    private ClientMagicState() {}

    public static boolean hasYin() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        PlayerData data = ClientPlayerData.of(player);
        return data != null && MagicSchool.YIN.acquiredBy(data);
    }
}
