package net.noiilive.hahueuh.network;

public final class ClientBaseShiftState {
    private static volatile boolean active;

    private ClientBaseShiftState() {}

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }

    public static void clear() {
        active = false;
    }
}
