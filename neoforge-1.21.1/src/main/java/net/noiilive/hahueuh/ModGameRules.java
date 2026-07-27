package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    private ModGameRules() {}

    public static GameRules.Key<GameRules.BooleanValue> REZERO_BLOCK_DESTRUCTION;
    public static GameRules.Key<GameRules.BooleanValue> REZERO_DESTRUCTION_DROPS;
    public static GameRules.Key<GameRules.IntegerValue> REZERO_DESTRUCTION_DROP_CHANCE;

    public static void register() {
        REZERO_BLOCK_DESTRUCTION = GameRules.register("rezeroBlockDestruction",
                GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        REZERO_DESTRUCTION_DROPS = GameRules.register("rezeroDestructionDrops",
                GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        REZERO_DESTRUCTION_DROP_CHANCE = GameRules.register("rezeroDestructionDropChance",
                GameRules.Category.MISC, GameRules.IntegerValue.create(50));
    }

    public static boolean rollDrops(ServerLevel level) {
        if (!level.getGameRules().getBoolean(REZERO_DESTRUCTION_DROPS)) return false;
        int chance = Math.clamp(level.getGameRules().getInt(REZERO_DESTRUCTION_DROP_CHANCE), 0, 100);
        if (chance >= 100) return true;
        if (chance <= 0) return false;
        return level.getRandom().nextInt(100) < chance;
    }
}
