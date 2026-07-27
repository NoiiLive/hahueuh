package net.noiilive.hahueuh.network;

public final class AllyTrackerData {
    private static volatile AllyDataPacket latest =
            new AllyDataPacket(false, 100.0f, 20.0f, 20.0f, 0.0, 0.0, 0.0, java.util.List.of(), java.util.List.of());
    private static volatile int version;
    private static volatile boolean openRequested;

    private AllyTrackerData() {}

    public static void accept(AllyDataPacket packet) {
        latest = packet;
        version++;
        if (packet.open) openRequested = true;
    }

    public static AllyDataPacket latest() {
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
