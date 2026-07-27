package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MiasmaTick {
    private static final int TICKS_PER_SECOND = 20;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (server.getTickCount() % TICKS_PER_SECOND != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!Miasma.hasActiveSinToggle(player)) continue;
            LevelChunk chunk = Miasma.chunkOf(player);
            if (chunk != null) {
                ChunkMiasmaData.add(chunk, ConfigMagic.MIASMA_PER_TOGGLE_SECOND.get());
            }
        }
    }
}
