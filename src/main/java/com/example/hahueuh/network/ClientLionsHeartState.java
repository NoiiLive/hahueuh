package com.example.hahueuh.network;

public final class ClientLionsHeartState {
    private static final long DOUBLE_TAP_WINDOW_MS = 300;

    private static volatile boolean active;
    private static volatile double floorLevel;
    private static volatile boolean descending;
    private static volatile boolean airWalkEnabled;

    private static boolean wasActive;
    private static boolean wasJumpDown;
    private static boolean wasGrounded;
    private static boolean wasDescending;
    private static long lastJumpPressTime;

    private ClientLionsHeartState() {}

    public static void setActive(boolean value) {
        active = value;
        if (!value) {
            wasActive = false;
            descending = false;
            airWalkEnabled = false;
        }
    }

    public static boolean isActive() {
        return active;
    }
    public static double floorLevel() {
        return floorLevel;
    }
    public static void lowerFloorLevel(double y) {
        if (y < floorLevel) floorLevel = y;
    }
    public static boolean isDescending() {
        return descending;
    }
    public static boolean isAirWalkEnabled() {
        return airWalkEnabled;
    }

    public static void updateFloor(double currentY, boolean onGround, boolean onRealGround,
                                    boolean jumpDown, boolean sneakDown, boolean submerged) {
        if (!active) {
            wasActive = false;
            return;
        }
        if (!wasActive) {
            floorLevel = currentY;
            wasActive = true;
            wasJumpDown = false;
            wasGrounded = false;
            wasDescending = false;
            airWalkEnabled = false;
            lastJumpPressTime = 0;
        }

        boolean jumpEdge = jumpDown && !wasJumpDown;
        wasJumpDown = jumpDown;

        if (jumpEdge) {
            long now = System.currentTimeMillis();
            if (lastJumpPressTime != 0 && now - lastJumpPressTime <= DOUBLE_TAP_WINDOW_MS) {
                airWalkEnabled = !airWalkEnabled;
                lastJumpPressTime = 0;
            } else {
                lastJumpPressTime = now;
            }
        }

        if (onRealGround) {
            airWalkEnabled = false;
        }

        descending = sneakDown || submerged;
        if (descending) {
            wasDescending = true;
            wasGrounded = onGround;
            return;
        }

        if (wasDescending) {
            floorLevel = currentY;
            wasDescending = false;
        } else if (onGround) {
            floorLevel = currentY;
        } else if (jumpEdge && wasGrounded) {
            floorLevel += 1.0;
        }
        wasGrounded = onGround;
    }

    public static void clear() {
        active = false;
        wasActive = false;
        wasJumpDown = false;
        wasGrounded = false;
        wasDescending = false;
        descending = false;
        airWalkEnabled = false;
        lastJumpPressTime = 0;
        floorLevel = 0;
    }
}
