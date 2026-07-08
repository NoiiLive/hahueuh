package com.example.hahueuh.network;

public final class DeathFadeState {
    public static final float FADE_SECONDS = 0.15f;

    private static volatile float target = 0f;
    private static float current = 0f;
    private static long lastNanos = 0L;

    private DeathFadeState() {}

    public static void onSignal(boolean toBlack) {
        target = toBlack ? 1f : 0f;
    }

    public static float advanceAndGetAlpha() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        dt = Math.min(dt, 0.1f);

        float rate = 1f / FADE_SECONDS;
        float t = target;
        if (current < t) current = Math.min(t, current + rate * dt);
        else if (current > t) current = Math.max(t, current - rate * dt);
        return current;
    }

    public static void reset() {
        target = 0f;
        current = 0f;
        lastNanos = 0L;
    }
}
