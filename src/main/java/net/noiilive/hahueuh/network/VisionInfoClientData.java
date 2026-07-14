package net.noiilive.hahueuh.network;

public final class VisionInfoClientData {
    private static volatile VisionInfoResultPayload latest;
    private static volatile int version;

    private VisionInfoClientData() {}

    public static void set(VisionInfoResultPayload payload) {
        latest = payload;
        version++;
    }

    public static VisionInfoResultPayload latest() {
        return latest;
    }

    public static int version() {
        return version;
    }

    public static void clear() {
        latest = null;
        version++;
    }
}
