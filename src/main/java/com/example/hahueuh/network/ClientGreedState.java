package com.example.hahueuh.network;

public final class ClientGreedState {
    private static volatile boolean canGreed;
    private static volatile GreedVariant greedVariant = GreedVariant.LIONSHEART;

    private ClientGreedState() {}

    public static void update(boolean canGreed, int greedVariantId) {
        ClientGreedState.canGreed = canGreed;
        ClientGreedState.greedVariant = GreedVariant.byOrdinal(greedVariantId);
    }

    public static boolean canGreed() {
        return canGreed;
    }

    public static GreedVariant greedVariant() {
        return greedVariant;
    }
}
