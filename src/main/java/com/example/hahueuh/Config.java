package com.example.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
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


    public static final ModConfigSpec.IntValue DOMAIN_RADIUS = BUILDER
            .comment("Full width (edge to edge, in blocks) of a Domain, centered on the Matrix (the",
                     "point where it was opened). So 100 means the dome spans 100 blocks across and",
                     "reaches 50 blocks in every direction; wandering past that closes the domain.",
                     "Default: 100. Range: 5 to 500.")
            .defineInRange("domainRadius", 100, 5, 500);

    public static final ModConfigSpec.IntValue DOMAIN_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after a Domain closes before that player can open another.",
                     "Applies however the domain closed (manual, leaving the radius, death, etc.).",
                     "Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("domainCooldownSeconds", 10, 0, 3600);


    public static final ModConfigSpec.IntValue SLOTH_MAX_DISTANCE = BUILDER
            .comment("Ceiling (in blocks) for how far the Unseen Hand can reach. Each Sloth user sets",
                     "their own reach with the scroll wheel, anywhere from 3 up to this cap.",
                     "Default: 16. Range: 3 to 100.")
            .defineInRange("slothMaxDistance", 16, 3, 100);

    public static final ModConfigSpec.IntValue SLOTH_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after the Unseen Hand is dismissed before it can be summoned",
                     "again. Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("slothCooldownSeconds", 10, 0, 3600);

    public static final ModConfigSpec.IntValue LIONS_HEART_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after Lion's Heart is toggled off before it can be used again.",
                     "Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("lionsHeartCooldownSeconds", 10, 0, 3600);

    public static final ModConfigSpec.BooleanValue SLOTH_COMPAT_ENABLED = BUILDER
            .comment("If true, Sloth users must earn 'compatibility' with the authority; using the Unseen",
                     "Hand before reaching the threshold inflicts drawbacks (blindness, hunger, nausea,",
                     "and steady damage). Disable to let anyone use Sloth freely. Default: true.")
            .define("slothCompatibilityEnabled", true);

    public static final ModConfigSpec.IntValue SLOTH_COMPAT_THRESHOLD = BUILDER
            .comment("Compatibility score a player must reach to use Sloth without drawbacks.",
                     "Default: 100. Range: 1 to 1000000.")
            .defineInRange("slothCompatibilityThreshold", 100, 1, 1_000_000);

    public static final ModConfigSpec.IntValue SLOTH_POINTS_TAMED_KILL = BUILDER
            .comment("Compatibility points gained when one of your TAMED entities kills a mob (letting",
                     "your pets do the work). Default: 5.")
            .defineInRange("slothPointsTamedKill", 5, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SLOTH_POINTS_VEHICLE_TRAVEL = BUILDER
            .comment("Compatibility points gained for every 30 seconds spent travelling while riding a",
                     "minecart, boat, horse, or pig instead of walking. Default: 1.")
            .defineInRange("slothPointsVehicleTravel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SLOTH_POINTS_NIGHT_SLEEP = BUILDER
            .comment("Compatibility points gained for sleeping through the night (skipping its hazards",
                     "rather than facing them). Default: 2.")
            .defineInRange("slothPointsNightSleep", 2, 0, 1_000_000);

    public static final ModConfigSpec.DoubleValue SEKHMET_MIN_SIZE = BUILDER
            .comment("Sekhmet variant: the smallest size multiplier its two giant hands can roll.",
                     "Each player is permanently assigned a random size between min and max, so their",
                     "Sekhmet feels personal. Bigger = more damage (2x size) and a wider grab reach.",
                     "Default: 3.0. Range: 1.0 to 8.0.")
            .defineInRange("sekhmetMinSize", 3.0, 1.0, 8.0);

    public static final ModConfigSpec.DoubleValue SEKHMET_MAX_SIZE = BUILDER
            .comment("Sekhmet variant: the largest size multiplier its two giant hands can roll.",
                     "Default: 6.0. Range: 1.0 to 8.0.")
            .defineInRange("sekhmetMaxSize", 6.0, 1.0, 8.0);

    public static final ModConfigSpec.IntValue UNSEEN_HANDS_MIN = BUILDER
            .comment("Unseen Hands variant: the fewest hands a player can be assigned. Each player gets a",
                     "permanent random count between min and max. Default: 5. Range: 1 to 100.")
            .defineInRange("unseenHandsMin", 5, 1, 100);

    public static final ModConfigSpec.IntValue UNSEEN_HANDS_MAX = BUILDER
            .comment("Unseen Hands variant: the most hands a player can be assigned.",
                     "Default: 15. Range: 1 to 100.")
            .defineInRange("unseenHandsMax", 15, 1, 100);

    public static final ModConfigSpec.IntValue UNSEEN_HANDS_MOBILITY_SPEED = BUILDER
            .comment("Unseen Hands variant: how fast (blocks/second) a player moves forward while in",
                     "mobility mode (activate the hand, then sneak, to plant all hands on the ground and",
                     "glide forward). Default: 8. Range: 1 to 40.")
            .defineInRange("unseenHandsMobilitySpeed", 8, 1, 40);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SEKHMET_BREAKABLE_TAGS = BUILDER
            .comment("Block tags Sekhmet's hands may smash while ATTACKING when the rezeroBlockDestruction",
                     "game rule is OFF (when ON, they break everything breakable). A block is breakable if",
                     "it belongs to ANY tag in this list. Default: shovel- and hoe-mineable blocks",
                     "(dirt/sand/gravel/clay/snow + leaves/crops/foliage). Use full tag ids, e.g.",
                     "\"minecraft:mineable/shovel\" or a modded tag.")
            .defineList("sekhmetBreakableTags",
                    List.of("minecraft:mineable/shovel", "minecraft:mineable/hoe"),
                    () -> "minecraft:mineable/shovel",
                    obj -> obj instanceof String s && ResourceLocation.tryParse(s) != null);


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
}
