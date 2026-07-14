package net.noiilive.hahueuh.network;

public final class ClientVisionOfLifeState {
    private static volatile boolean active;

    private ClientVisionOfLifeState() {}

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
