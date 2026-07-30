package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.UlMinyaActivatePacket;

public final class UlMinyaClient {
    private UlMinyaClient() {}

    public static void activate() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Entity target = AlShamakClient.raycastEntity(player, net.noiilive.hahueuh.network.ClientConfigValues.ulMinyaRange());
        ModNetworking.CHANNEL.sendToServer(new UlMinyaActivatePacket(target != null ? target.getId() : -1));
    }
}
