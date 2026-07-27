package net.noiilive.hahueuh.network;

import net.minecraft.world.InteractionHand;

public final class ClientDualWieldState {
    private static volatile boolean offhandNext;

    private ClientDualWieldState() {}

    public static void setOffhandNext(boolean value) {
        offhandNext = value;
    }

    public static boolean offhandNext() {
        return offhandNext;
    }

    public static InteractionHand takeSwingHand() {
        InteractionHand hand = offhandNext ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        offhandNext = !offhandNext;
        return hand;
    }

    public static void clear() {
        offhandNext = false;
    }
}
