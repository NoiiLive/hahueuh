package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ConfigGreed {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue LIONS_HEART_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after Lion's Heart is toggled off before it can be used again.",
                     "Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("lionsHeartCooldownSeconds", 10, 0, 3600);

    public static final ModConfigSpec.IntValue LIONS_HEART_DURATION_MIN_SECONDS = BUILDER
            .comment("Minimum duration (in seconds) Lion's Heart can be held active before the user's",
                     "heart begins to strain and burn out. Each activation picks a random duration between",
                     "this and the maximum, so the safe window varies per use. The burnout-to-death phase",
                     "afterward always lasts exactly as long as the duration that was rolled.",
                     "Default: 5. Range: 1 to 3600.")
            .defineInRange("lionsHeartDurationMinSeconds", 5, 1, 3600);

    public static final ModConfigSpec.IntValue LIONS_HEART_DURATION_MAX_SECONDS = BUILDER
            .comment("Maximum duration (in seconds) Lion's Heart can be held active before the user's",
                     "heart begins to strain and burn out. Default: 10. Range: 1 to 3600.")
            .defineInRange("lionsHeartDurationMaxSeconds", 10, 1, 3600);


    public static final ModConfigSpec.IntValue LITTLE_KING_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) between Little King implants. Default: 60. Range: 0 to 3600.")
            .defineInRange("littleKingCooldownSeconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue LITTLE_KING_RANGE_BLOCKS = BUILDER
            .comment("How far (in blocks) an implanted target may stray before its heart stops extending",
                     "the Lion's Heart timer. The heart stays implanted regardless; it just goes inactive",
                     "past this range, and any single out-of-range heart drops the indefinite status.",
                     "Default: 250. Range: 1 to 10000.")
            .defineInRange("littleKingRangeBlocks", 250, 1, 10000);

    public static final ModConfigSpec.BooleanValue LITTLE_KING_INDEFINITE = BUILDER
            .comment("If true, once every possible heart has been implanted (the king is down to half a",
                     "heart of max health) AND all of those hearts are within range, Lion's Heart never",
                     "burns out. If false, the king instead just gets the accumulated duration bonus: each",
                     "in-range heart adds the base rolled duration again (e.g. -19 health, a 5s roll gives",
                     "5 + 19*5 = 100s total). Default: true.")
            .define("littleKingIndefinite", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LITTLE_KING_IMPLANTABLE_ENTITIES = BUILDER
            .comment("Entity types a Little King may implant a heart into. Use full entity type ids, e.g.",
                     "\"minecraft:player\" or \"minecraft:villager\". Default: players and villagers.")
            .defineList("littleKingImplantableEntities",
                    List.of("minecraft:player", "minecraft:villager"),
                    () -> "minecraft:villager",
                    obj -> obj instanceof String s && ResourceLocation.tryParse(s) != null);


    public static final ModConfigSpec.IntValue MATERIAL_PHASE_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after Material Phase is toggled off before it can be used again.",
                     "Set to 0 to disable. Default: 5. Range: 0 to 3600.")
            .defineInRange("materialPhaseCooldownSeconds", 5, 0, 3600);


    public static final ModConfigSpec.IntValue OBJECT_FREEZE_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) between Object Freeze uses. Set to 0 to disable.",
                     "Default: 5. Range: 0 to 3600.")
            .defineInRange("objectFreezeCooldownSeconds", 5, 0, 3600);

    public static final ModConfigSpec.IntValue GREED_PROJECTILE_DISTANCE = BUILDER
            .comment("How far (in blocks) an Object Freeze thrown-object projectile can fly before it's",
                     "forcibly discarded, so a high-speed shot can't outrun render/simulation distance and",
                     "linger somewhere off-screen. Default: 32. Range: 1 to 1000.")
            .defineInRange("greedProjectileDistance", 32, 1, 1000);


    public static final ModConfigSpec.IntValue ALLY_TRACKER_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) between Ally Tracker marks (opening the tracker GUI is always",
                     "free and never triggers a cooldown). Set to 0 to disable. Default: 15. Range: 0 to 3600.")
            .defineInRange("allyTrackerCooldownSeconds", 15, 0, 3600);

    public static final ModConfigSpec.IntValue ALLY_TRACKER_MAX_ALLIES = BUILDER
            .comment("Maximum number of allies a Cor Leonis user may register with Ally Tracker.",
                     "Default: 100. Range: 1 to 10000.")
            .defineInRange("allyTrackerMaxAllies", 100, 1, 10000);

    public static final ModConfigSpec.IntValue BASE_SHIFT_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after Base Shift or Second Shift is toggled off before that same",
                     "ability can be used again. Base Shift and Second Shift each track their own independent",
                     "cooldown timer, but share this one config value, since they're opposite phases of the",
                     "same ability. Set to 0 to disable. Default: 30. Range: 0 to 3600.")
            .defineInRange("baseShiftCooldownSeconds", 30, 0, 3600);

    public static final ModConfigSpec.DoubleValue SECOND_SHIFT_MIN_EFFECT_SHARE = BUILDER
            .comment("The minimum split share (as a fraction of one full effect level, e.g. 0.1 = 10%) a Second",
                     "Shift participant must be allotted for a shared potion effect to reach them at all — below",
                     "this, that participant simply doesn't get the effect. Default: 0.1. Range: 0.0 to 1.0.")
            .defineInRange("secondShiftMinEffectShare", 0.1, 0.0, 1.0);

    public static final ModConfigSpec.IntValue GREED_POINTS_CHEST_LOOT = BUILDER
            .comment("Greed compatibility points gained the first time a generated (never-opened) chest or",
                     "minecart chest is looted. Default: 5.")
            .defineInRange("greedPointsChestLoot", 5, 0, 1_000_000);

    public static final ModConfigSpec.IntValue GREED_POINTS_ORE_MINE = BUILDER
            .comment("Greed compatibility points gained per diamond, emerald, or ancient debris ore mined.",
                     "Default: 2.")
            .defineInRange("greedPointsOreMine", 2, 0, 1_000_000);

    public static final ModConfigSpec.IntValue GREED_POINTS_VILLAGER_TRADE = BUILDER
            .comment("Greed compatibility points gained per villager trade that pays out emeralds (selling",
                     "to the villager, not buying with them). Default: 1.")
            .defineInRange("greedPointsVillagerTrade", 1, 0, 1_000_000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigGreed() {}
}
