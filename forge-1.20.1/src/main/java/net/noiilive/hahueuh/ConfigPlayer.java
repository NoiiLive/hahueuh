package net.noiilive.hahueuh;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigPlayer {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AGING_ENABLED = BUILDER
            .comment("If true, players age up over time (see ageUpIntervalDays). Set false to freeze every",
                     "player's age where it stands — they simply never age up. This does NOT touch",
                     "lifespans, so max Od/Mana (derived from Lifespan) are unaffected; it only stops the",
                     "age counter, which in turn means nobody dies of old age naturally. Default: true.")
            .define("agingEnabled", true);

    public static final ForgeConfigSpec.IntValue STARTING_AGE = BUILDER
            .comment("The age a player starts at the first time they're ever seen, and the age they reset",
                     "to after dying of old age without Domain/Return by Death protection on a non-hardcore",
                     "world. Default: 16. Range: 0 to 1000000.")
            .defineInRange("startingAge", 16, 0, 1_000_000);

    public static final ForgeConfigSpec.IntValue AGE_UP_INTERVAL_DAYS = BUILDER
            .comment("How many in-game days must pass before a player's Book of Life age (see the Book of",
                     "Life screen, default 16) increases by 1. Measured against the overworld's day counter,",
                     "not per-player playtime — a player who logs off ages up for the days that passed while",
                     "they were away too, caught up the next time they're seen online. Default: 30.",
                     "Range: 1 to 365.")
            .defineInRange("ageUpIntervalDays", 30, 1, 365);

    public static final ForgeConfigSpec.IntValue HUMAN_LIFESPAN_MIN = BUILDER
            .comment("Lifespan (in years) for a Human is rolled uniformly between this and",
                     "humanLifespanMax, inclusive, the first time a player is seen with that race and",
                     "again each time their race changes to Human. Default: 80. Range: 1 to 1000000.")
            .defineInRange("humanLifespanMin", 80, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue HUMAN_LIFESPAN_MAX = BUILDER
            .comment("See humanLifespanMin. Default: 120. Range: 1 to 1000000.")
            .defineInRange("humanLifespanMax", 120, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue ELF_LIFESPAN_MIN = BUILDER
            .comment("See humanLifespanMin — same, but for the Elf race. Default: 800. Range: 1 to 1000000.")
            .defineInRange("elfLifespanMin", 800, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue ELF_LIFESPAN_MAX = BUILDER
            .comment("See elfLifespanMin. Default: 1000. Range: 1 to 1000000.")
            .defineInRange("elfLifespanMax", 1000, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue HALF_ELF_LIFESPAN_MIN = BUILDER
            .comment("See humanLifespanMin — same, but for the Half Elf race. Default: 300. Range: 1 to 1000000.")
            .defineInRange("halfElfLifespanMin", 300, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue HALF_ELF_LIFESPAN_MAX = BUILDER
            .comment("See halfElfLifespanMin. Default: 500. Range: 1 to 1000000.")
            .defineInRange("halfElfLifespanMax", 500, 1, 1_000_000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConfigPlayer() {}
}
