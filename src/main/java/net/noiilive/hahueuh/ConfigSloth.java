package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ConfigSloth {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SLOTH_MAX_DISTANCE = BUILDER
            .comment("Ceiling (in blocks) for how far the Unseen Hand can reach. Each Sloth user sets",
                     "their own reach with the scroll wheel, anywhere from 3 up to this cap.",
                     "Default: 16. Range: 3 to 100.")
            .defineInRange("slothMaxDistance", 16, 3, 100);

    public static final ModConfigSpec.IntValue SLOTH_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after the Unseen Hand is dismissed before it can be summoned",
                     "again. Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("slothCooldownSeconds", 10, 0, 3600);

    public static final ModConfigSpec.IntValue QUICK_ACTION_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) shared by the quick-use Sloth abilities — Quick Strike, Quick",
                     "Grasp, Hidden Interaction, and Self Propel — after one finishes before any of the",
                     "four can be used again. Independent from the Summon Hand cooldown above. Set to 0 to",
                     "disable. Default: 5. Range: 0 to 3600.")
            .defineInRange("quickActionCooldownSeconds", 5, 0, 3600);

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

    public static final ModConfigSpec.IntValue VARIANT_WEIGHT_INVISIBLE_PROVIDENCE = BUILDER
            .comment("Relative weight for rolling Invisible Providence when a player absorbs a Sloth Witch",
                     "Factor (see SnapshotManager/WitchFactorGrant) — weighed against",
                     "variantWeightUnseenHands/variantWeightSekhmet, not a percentage on its own (e.g. 55/40/5",
                     "out of 100 total is the same odds as 11/8/1 out of 20). Default: 55.")
            .defineInRange("variantWeightInvisibleProvidence", 55, 0, 1_000_000);

    public static final ModConfigSpec.IntValue VARIANT_WEIGHT_UNSEEN_HANDS = BUILDER
            .comment("See variantWeightInvisibleProvidence. Default: 40.")
            .defineInRange("variantWeightUnseenHands", 40, 0, 1_000_000);

    public static final ModConfigSpec.IntValue VARIANT_WEIGHT_SEKHMET = BUILDER
            .comment("See variantWeightInvisibleProvidence. Default: 5.")
            .defineInRange("variantWeightSekhmet", 5, 0, 1_000_000);

    public static final ModConfigSpec.DoubleValue MOB_QUICK_ACTION_CHANCE = BUILDER
            .comment("Chance, rolled about once a second while a mob holding a Sloth Witch Factor is actively",
                     "fighting someone, that it uses Quick Strike or Quick Grasp (picked randomly) on its",
                     "target — the only two Sloth moves mobs use (Summon Hand's hold-to-aim doesn't suit them,",
                     "and Hidden Interaction/Self Propel are Invisible Providence/Unseen Hands-only utility",
                     "moves with no combat use). Reach-limited by slothMaxDistance and shares",
                     "quickActionCooldownSeconds with the player version. Default: 15. Range: 0 to 100.")
            .defineInRange("mobQuickActionChance", 15.0, 0.0, 100.0);

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

    static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigSloth() {}
}
