package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ProxiedInteractionSound {
    private static ServerPlayer proxyingFor;

    private ProxiedInteractionSound() {}

    public static void begin(ServerPlayer owner) {
        proxyingFor = owner;
    }

    public static void end() {
        proxyingFor = null;
    }

    public static Player unmask(Level level, Player excluded) {
        ServerPlayer owner = proxyingFor;
        if (owner == null || excluded != owner) return excluded;
        if (level != owner.level()) return excluded;
        return null;
    }
}
