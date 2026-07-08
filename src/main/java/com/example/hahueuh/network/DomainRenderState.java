package com.example.hahueuh.network;

import net.minecraft.resources.ResourceLocation;

public final class DomainRenderState {
    public static final float FADE_SECONDS = 0.4f;

    private static volatile boolean active;
    private static volatile double x, y, z, radius;
    private static volatile ResourceLocation dimension;

    private static float currentAlpha;
    private static long lastNanos;

    private DomainRenderState() {}

    public static void update(DomainStatePayload p) {
        if (p.active()) {
            x = p.x();
            y = p.y();
            z = p.z();
            radius = p.radius();
            dimension = p.dimension();
            active = true;
        } else {
            active = false;
        }
    }

    public static void clear() {
        active = false;
        currentAlpha = 0f;
        lastNanos = 0L;
    }

    public static float advanceAndGetAlpha() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        dt = Math.min(dt, 0.1f);

        float target = active ? 1f : 0f;
        float rate = 1f / FADE_SECONDS;
        if (currentAlpha < target) currentAlpha = Math.min(target, currentAlpha + rate * dt);
        else if (currentAlpha > target) currentAlpha = Math.max(target, currentAlpha - rate * dt);
        return currentAlpha;
    }

    public static boolean isActive() { return active; }
    public static double x() { return x; }
    public static double y() { return y; }
    public static double z() { return z; }
    public static double radius() { return radius; }
    public static ResourceLocation dimension() { return dimension; }
}
