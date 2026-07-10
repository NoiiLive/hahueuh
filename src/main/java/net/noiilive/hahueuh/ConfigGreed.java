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

    static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigGreed() {}
}
