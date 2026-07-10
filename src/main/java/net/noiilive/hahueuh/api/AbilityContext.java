package net.noiilive.hahueuh.api;

import net.minecraft.world.entity.player.Player;

public interface AbilityContext {
    Player player();

    boolean isOnCooldown();

    int cooldownSecondsRemaining();

    void startCooldown(double seconds);

    boolean anyBoundSlotDown();
}
