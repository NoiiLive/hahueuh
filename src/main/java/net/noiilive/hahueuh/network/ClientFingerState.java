package net.noiilive.hahueuh.network;

public final class ClientFingerState {
    private static volatile int hands;

    private ClientFingerState() {}

    public static void setHands(int value) {
        hands = Math.max(0, value);
    }

    public static int hands() {
        return hands;
    }

    public static boolean hasHands() {
        return hands > 0;
    }
}
