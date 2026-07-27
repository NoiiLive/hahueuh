package net.noiilive.hahueuh.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.noiilive.hahueuh.capability.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayerData {
    private static final Map<UUID, PlayerData> CACHE = new ConcurrentHashMap<>();

    private ClientPlayerData() {}

    public static void accept(UUID uuid, CompoundTag tag) {
        if (tag == null) return;
        PlayerData data = CACHE.computeIfAbsent(uuid, k -> new PlayerData());
        data.deserializeNBT(tag);
    }

    public static PlayerData of(Player player) {
        return player == null ? new PlayerData() : of(player.getUUID());
    }

    public static PlayerData of(UUID uuid) {
        return CACHE.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public static boolean has(UUID uuid) {
        return CACHE.containsKey(uuid);
    }

    public static void clear() {
        CACHE.clear();
    }
}
