package com.example.hahueuh.client;

import com.example.hahueuh.Config;
import com.example.hahueuh.network.ClientSlothState;
import com.example.hahueuh.network.HandMode;

public final class UnseenHandState {
    public static final double MIN_RANGE = 3.0;

    private static volatile boolean active;
    private static volatile boolean serverActive;
    private static volatile HandMode mode = HandMode.NONE;
    private static volatile double maxRange = 8.0;
    private static volatile double liveDistance;
    private static volatile boolean mobility;

    private UnseenHandState() {}

    public static boolean isActive() { return active; }
    public static void setActive(boolean value) { active = value; }

    public static boolean isServerActive() { return serverActive; }
    public static void setServerActive(boolean value) { serverActive = value; }

    public static HandMode mode() { return mode; }
    public static void setMode(HandMode value) { mode = value; }

    public static double maxRange() {
        return Math.max(MIN_RANGE, Math.min(configCeiling(), maxRange));
    }

    public static double adjustMaxRange(double scrollDeltaY) {
        maxRange = Math.max(MIN_RANGE, Math.min(configCeiling(), maxRange + scrollDeltaY));
        return maxRange;
    }

    public static double liveDistance() { return liveDistance; }
    public static void setLiveDistance(double value) { liveDistance = value; }

    public static boolean isMobility() { return mobility; }
    public static void setMobility(boolean value) { mobility = value; }

    private static double configCeiling() {
        return Config.SLOTH_MAX_DISTANCE.getAsInt() * ClientSlothState.slothVariant().reachMultiplier;
    }
}
