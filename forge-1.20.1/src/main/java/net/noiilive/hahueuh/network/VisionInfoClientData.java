package net.noiilive.hahueuh.network;

public final class VisionInfoClientData {
    private static volatile VisionInfoResultPacket latest;
    private static volatile int version;

    private VisionInfoClientData() {}

    public static void set(VisionInfoResultPacket payload) {
        latest = payload;
        version++;
    }

    public static VisionInfoResultPacket latest() {
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
