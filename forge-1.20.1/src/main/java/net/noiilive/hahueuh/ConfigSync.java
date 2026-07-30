package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.noiilive.hahueuh.network.ConfigSyncPacket;
import net.noiilive.hahueuh.network.ModNetworking;

public final class ConfigSync {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.sendToPlayer(player, ConfigSyncPacket.current());
        }
    }

    public static void broadcast() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        ModNetworking.sendToAll(ConfigSyncPacket.current());
    }
}
