package net.noiilive.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class AbilityCooldowns {
    private static final Map<ResourceLocation, Long> cooldownUntilMs = new HashMap<>();

    private AbilityCooldowns() {}

    public static void startCooldown(ResourceLocation abilityId, double seconds) {
        if (seconds <= 0) {
            cooldownUntilMs.remove(abilityId);
            return;
        }
        cooldownUntilMs.put(abilityId, System.currentTimeMillis() + (long) (seconds * 1000));
    }

    public static int secondsRemaining(ResourceLocation abilityId) {
        Long until = cooldownUntilMs.get(abilityId);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }

    public static void reset() {
        cooldownUntilMs.clear();
    }
}
