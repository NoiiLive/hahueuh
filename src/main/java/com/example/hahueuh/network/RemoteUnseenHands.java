package com.example.hahueuh.network;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteUnseenHands {
    public record Remote(float distance, int mode, int variant, boolean mobility) {}

    private static final Map<UUID, Remote> HANDS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Integer>> GRABBED = new ConcurrentHashMap<>();

    private RemoteUnseenHands() {}

    public static void update(UUID owner, boolean active, float distance, int mode, int variant, boolean mobility) {
        if (active) {
            HANDS.put(owner, new Remote(distance, mode, variant, mobility));
        } else {
            HANDS.remove(owner);
        }
    }

    public static Map<UUID, Remote> active() {
        return HANDS;
    }

    public static void updateGrabbed(UUID owner, List<Integer> grabbedIds) {
        GRABBED.put(owner, grabbedIds);
    }

    public static List<Integer> grabbedFor(UUID owner) {
        return GRABBED.getOrDefault(owner, List.of());
    }

    public static void clear() {
        HANDS.clear();
        GRABBED.clear();
    }
}
