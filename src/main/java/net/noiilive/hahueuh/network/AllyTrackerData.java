package net.noiilive.hahueuh.network;

public final class AllyTrackerData {
    private static volatile AllyDataPayload latest =
            new AllyDataPayload(false, 100.0f, 20.0f, 20.0f, 0.0, 0.0, 0.0, java.util.List.of(), java.util.List.of());
    private static volatile int version;
    private static volatile boolean openRequested;

    private AllyTrackerData() {}

    public static void receive(AllyDataPayload payload) {
        latest = payload;
        version++;
        if (payload.open()) openRequested = true;
    }

    public static AllyDataPayload latest() {
        return latest;
    }

    public static int version() {
        return version;
    }

    public static boolean consumeOpenRequest() {
        if (openRequested) {
            openRequested = false;
            return true;
        }
        return false;
    }
}
