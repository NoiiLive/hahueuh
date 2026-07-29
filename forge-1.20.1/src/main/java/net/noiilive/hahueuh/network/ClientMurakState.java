package net.noiilive.hahueuh.network;

public final class ClientMurakState {
    private static volatile boolean reducedGravity;
    private static volatile boolean flying;

    private ClientMurakState() {}

    public static void update(boolean reducedGravity, boolean flying) {
        ClientMurakState.reducedGravity = reducedGravity;
        ClientMurakState.flying = flying;
    }

    public static boolean hasReducedGravity() {
        return reducedGravity;
    }

    public static boolean isFlying() {
        return flying;
    }

    public static void clear() {
        reducedGravity = false;
        flying = false;
    }
}
