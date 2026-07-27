package net.noiilive.hahueuh.network;

import java.util.List;

public final class ClientFootprintState {
    private static volatile List<FootprintSyncPayload.Footprint> footprints = List.of();
    private static volatile int maxAgeTicks = 600;

    private ClientFootprintState() {}

    public static void set(int maxAge, List<FootprintSyncPayload.Footprint> list) {
        maxAgeTicks = maxAge;
        footprints = list;
    }

    public static List<FootprintSyncPayload.Footprint> footprints() {
        return footprints;
    }

    public static int maxAgeTicks() {
        return maxAgeTicks;
    }

    public static boolean isEmpty() {
        return footprints.isEmpty();
    }

    public static void clear() {
        footprints = List.of();
    }
}
