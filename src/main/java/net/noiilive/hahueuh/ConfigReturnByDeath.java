package net.noiilive.hahueuh;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigReturnByDeath {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CHECKPOINT_TIMER_ENABLED = BUILDER
            .comment("If true, checkpoints are periodically created on a timer.",
                     "Default: true.")
            .define("checkpointTimerEnabled", true);

    public static final ModConfigSpec.IntValue CHECKPOINT_INTERVAL_SECONDS = BUILDER
            .comment("How often (in seconds) the world creates a checkpoint.",
                     "On death, the entire world reverts to the last checkpoint.",
                     "Lower values = more frequent saves, higher safety, but more disk I/O.",
                     "Default: 30 seconds. Range: 5 seconds to 24 hours.")
            .defineInRange("checkpointIntervalSeconds", 30, 5, 86400);

    public static final ModConfigSpec.IntValue CHECKPOINT_INTERVAL_RANDOMNESS_SECONDS = BUILDER
            .comment("Adds random jitter to the timer-based checkpoint interval, so saves don't",
                     "happen at a perfectly predictable cadence.",
                     "Each checkpoint's actual interval is checkpointIntervalSeconds +/- a random",
                     "amount up to this value. E.g. with an interval of 30 and randomness of 5,",
                     "checkpoints happen every 25-35 seconds.",
                     "Set to 0 to disable and use a fixed interval. Default: 5 seconds.")
            .defineInRange("checkpointIntervalRandomnessSeconds", 5, 0, 86400);

    public static final ModConfigSpec.IntValue CHECKPOINT_TIMER_CHANCE = BUILDER
            .comment("Percent chance (0-100) that a checkpoint actually gets created each time the",
                     "timer interval elapses. E.g. 50 means only half of timer-triggered checkpoints",
                     "actually happen. Default: 100 (always).")
            .defineInRange("checkpointTimerChance", 100, 0, 100);


    public static final ModConfigSpec.BooleanValue CHECKPOINT_ON_ADVANCEMENT_ENABLED = BUILDER
            .comment("If true, earning an advancement (achievement) has a chance to create a checkpoint.",
                     "Default: true.")
            .define("checkpointOnAdvancementEnabled", true);

    public static final ModConfigSpec.IntValue CHECKPOINT_ON_ADVANCEMENT_CHANCE = BUILDER
            .comment("Percent chance (0-100) that earning an advancement creates a checkpoint.",
                     "Default: 100 (always).")
            .defineInRange("checkpointOnAdvancementChance", 100, 0, 100);


    public static final ModConfigSpec.BooleanValue CHECKPOINT_ON_SLEEP_ENABLED = BUILDER
            .comment("If true, waking up from sleeping in a bed has a chance to create a checkpoint.",
                     "Default: true.")
            .define("checkpointOnSleepEnabled", true);

    public static final ModConfigSpec.IntValue CHECKPOINT_ON_SLEEP_CHANCE = BUILDER
            .comment("Percent chance (0-100) that waking up from sleep creates a checkpoint.",
                     "Default: 100 (always).")
            .defineInRange("checkpointOnSleepChance", 100, 0, 100);


    public static final ModConfigSpec.BooleanValue WITCH_MIASMA_ENABLED = BUILDER
            .comment("If true, using Return by Death inflicts a stacking 'Witch's Miasma' effect that",
                     "draws nearby hostile mobs toward the player. Creative/spectator players are always",
                     "excluded. Default: true.")
            .define("witchMiasmaEnabled", true);

    public static final ModConfigSpec.IntValue WITCH_MIASMA_MAX_LEVEL = BUILDER
            .comment("Highest level Witch's Miasma can stack to (level I = 1). Default: 5. Range: 1 to 20.")
            .defineInRange("witchMiasmaMaxLevel", 5, 1, 20);


    public static final ModConfigSpec.BooleanValue SHOW_CHECKPOINT_NOTIFICATION = BUILDER
            .comment("If true, a chat message will be sent to all players when a checkpoint is saved.",
                     "If false, checkpoints are silent — no messages or sounds.",
                     "Default: false.")
            .define("showCheckpointNotification", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigReturnByDeath() {}
}
