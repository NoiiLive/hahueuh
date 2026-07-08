package com.example.hahueuh.network;

public final class ClientSlothState {
    private static volatile boolean canSloth;
    private static volatile SlothVariant slothVariant = SlothVariant.INVISIBLE_PROVIDENCE;

    private ClientSlothState() {}

    public static void update(boolean canSloth, int slothVariantId) {
        ClientSlothState.canSloth = canSloth;
        ClientSlothState.slothVariant = SlothVariant.byOrdinal(slothVariantId);
    }

    public static boolean canSloth() {
        return canSloth;
    }

    public static SlothVariant slothVariant() {
        return slothVariant;
    }
}
