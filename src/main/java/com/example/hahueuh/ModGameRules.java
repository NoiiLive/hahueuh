package com.example.hahueuh;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    private ModGameRules() {}

    public static GameRules.Key<GameRules.BooleanValue> REZERO_BLOCK_DESTRUCTION;

    public static void register() {
        REZERO_BLOCK_DESTRUCTION = GameRules.register("rezeroBlockDestruction",
                GameRules.Category.MISC, GameRules.BooleanValue.create(true));
    }
}
