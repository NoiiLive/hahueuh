package net.noiilive.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AbilityCooldowns {
    private static final Map<ResourceLocation, Integer> remainingTicks = new HashMap<>();

    private AbilityCooldowns() {}

    public static void startCooldown(ResourceLocation abilityId, double seconds) {
        if (seconds <= 0) {
            remainingTicks.remove(abilityId);
            return;
        }
        remainingTicks.put(abilityId, (int) Math.ceil(seconds * 20.0));
    }

    public static void tick() {
        if (remainingTicks.isEmpty()) return;
        Iterator<Map.Entry<ResourceLocation, Integer>> it = remainingTicks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry = it.next();
            int left = entry.getValue() - 1;
            if (left <= 0) {
                it.remove();
            } else {
                entry.setValue(left);
            }
        }
    }

    public static int secondsRemaining(ResourceLocation abilityId) {
        Integer left = remainingTicks.get(abilityId);
        if (left == null || left <= 0) return 0;
        return (int) Math.ceil(left / 20.0);
    }

    public static void reset() {
        remainingTicks.clear();
    }
}
