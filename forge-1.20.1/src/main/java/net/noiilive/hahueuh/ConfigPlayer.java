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

    public static final ForgeConfigSpec.IntValue STAT_PROFICIENCY_MAX = BUILDER
            .comment("Player stats: upper bound of the Proficiency roll each stat gets at creation. Proficiency",
                     "scales how much every level of that stat is worth, so raising this raises the ceiling",
                     "on how strong a trained stat can become. Rolls 1 to this value inclusive.",
                     "Default: 10. Range: 1 to 1000.")
            .defineInRange("statProficiencyMax", 10, 1, 1000);

    public static final ForgeConfigSpec.IntValue STAT_CAPACITY_MAX = BUILDER
            .comment("Player stats: upper bound of the Capacity roll each stat gets at creation. Capacity sets",
                     "the level you start that stat at and, via statCapPerCapacity, the level it caps out at.",
                     "Rolls 1 to this value inclusive. Default: 10. Range: 1 to 1000.")
            .defineInRange("statCapacityMax", 10, 1, 1000);

    public static final ForgeConfigSpec.IntValue STAT_PROGRESS_PER_LEVEL = BUILDER
            .comment("Player stats: how much progress a stat needs to gain a level. The bar in the Book of Life",
                     "reads progress out of this number. Default: 100. Range: 1 to 1000000.")
            .defineInRange("statProgressPerLevel", 100, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue STAT_CAP_PER_CAPACITY = BUILDER
            .comment("Player stats: how much level cap each point of Capacity grants. A Capacity of 10 with the",
                     "default 10 here caps that stat at level 100 — no amount of training pushes past it.",
                     "Default: 10. Range: 1 to 1000.")
            .defineInRange("statCapPerCapacity", 10, 1, 1000);

    public static final ForgeConfigSpec.DoubleValue TENACITY_MAX_HEALTH_BONUS = BUILDER
            .comment("Tenacity: bonus max health at a perfect stat, as a fraction.",
                     "At 1.125 and level 100 — Proficiency 10: +112.5%, 20 -> 42.5 HP (21.25 hearts).",
                     "                          Proficiency 1:  +11.25%, 20 -> 22.25 HP (11.1 hearts).",
                     "Default: 1.125. Range: 0 to 10.")
            .defineInRange("tenacityMaxHealthBonus", 1.125, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue TENACITY_REGEN_BONUS = BUILDER
            .comment("Tenacity: bonus natural healing at a perfect stat, as a fraction.",
                     "At 1.5 and level 100 — Proficiency 10: healing x2.5, so a 1 HP regen tick heals 2.5.",
                     "                       Proficiency 1:  healing x1.15, so that tick heals 1.15.",
                     "Default: 1.5. Range: 0 to 10.")
            .defineInRange("tenacityRegenBonus", 1.5, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue FORTITUDE_DAMAGE_REDUCTION = BUILDER
            .comment("Fortitude: incoming damage reduction at a perfect stat, as a fraction. Applied before",
                     "armour, and range-capped below 1.0 so this can never make you immune.",
                     "At 0.525 and level 100 — Proficiency 10: 52.5% less, 10 raw damage becomes 4.75.",
                     "                         Proficiency 1:  5.25% less, 10 raw damage becomes 9.48.",
                     "Default: 0.525. Range: 0 to 0.9.")
            .defineInRange("fortitudeDamageReduction", 0.525, 0.0, 0.9);

    public static final ForgeConfigSpec.DoubleValue STRENGTH_FIST_DAMAGE_BONUS = BUILDER
            .comment("Strength: bonus unarmed damage at a perfect stat, as a fraction. Bare hands only —",
                     "weapons scale off combatDamageBonus instead.",
                     "At 3.0 and level 100 — Proficiency 10: +300%, a 1-damage punch deals 4.0 (6.0 crit).",
                     "                       Proficiency 1:  +30%, that punch deals 1.3 (1.95 crit).",
                     "Default: 3.0. Range: 0 to 20.")
            .defineInRange("strengthFistDamageBonus", 3.0, 0.0, 20.0);

    public static final ForgeConfigSpec.DoubleValue STRENGTH_JUMP_BONUS = BUILDER
            .comment("Strength: bonus jump strength at a perfect stat, as a fraction.",
                     "At 0.6 and level 100 — Proficiency 10: +60%, 0.42 -> 0.672, roughly 3.2 blocks high.",
                     "                       Proficiency 1:  +6%, 0.42 -> 0.4452, roughly 1.4 blocks high.",
                     "Default: 0.6. Range: 0 to 5.")
            .defineInRange("strengthJumpBonus", 0.6, 0.0, 5.0);

    public static final ForgeConfigSpec.DoubleValue STRENGTH_FALL_REDUCTION = BUILDER
            .comment("Strength: fall damage reduction at a perfect stat, as a fraction.",
                     "At 0.75 and level 100 — Proficiency 10: fall damage x0.25, a 20-block fall deals 4.25.",
                     "                        Proficiency 1:  fall damage x0.925, that fall deals 15.7.",
                     "Note the high end plus Tenacity's health makes falls survivable from almost any",
                     "height; lower this to around 0.6 if you want falling to stay a real threat.",
                     "Default: 0.75. Range: 0 to 1.")
            .defineInRange("strengthFallReduction", 0.75, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue REFLEXES_SPEED_BONUS = BUILDER
            .comment("Reflexes: bonus movement speed at a perfect stat, as a fraction. Speed compounds hard",
                     "in Minecraft, so this is kept modest.",
                     "At 0.375 and level 100 — Proficiency 10: +37.5%, sprint 5.6 -> roughly 7.7 blocks/sec.",
                     "                         Proficiency 1:  +3.75%, sprint 5.6 -> roughly 5.8 blocks/sec.",
                     "Default: 0.375. Range: 0 to 5.")
            .defineInRange("reflexesSpeedBonus", 0.375, 0.0, 5.0);

    public static final ForgeConfigSpec.DoubleValue REFLEXES_ATTACK_SPEED_BONUS = BUILDER
            .comment("Reflexes: bonus attack speed while empty-handed at a perfect stat, as a fraction.",
                     "At 0.75 and level 100 — Proficiency 10: 4.0 -> 7.0, a swing every 0.14s (vanilla 0.25s).",
                     "                        Proficiency 1:  4.0 -> 4.3, a swing every 0.23s.",
                     "Default: 0.75. Range: 0 to 5.")
            .defineInRange("reflexesAttackSpeedBonus", 0.75, 0.0, 5.0);

    public static final ForgeConfigSpec.DoubleValue MAGIC_CAPACITY_BONUS = BUILDER
            .comment("Magic Mastery: bonus max mana at a perfect stat, as a fraction.",
                     "At 1.5 and level 100 — Proficiency 10: +150%, a 200 mana pool becomes 500.",
                     "                       Proficiency 1:  +15%, that pool becomes 230.",
                     "Default: 1.5. Range: 0 to 20.")
            .defineInRange("magicCapacityBonus", 1.5, 0.0, 20.0);

    public static final ForgeConfigSpec.DoubleValue MAGIC_CHARGE_BONUS = BUILDER
            .comment("Magic Mastery: bonus mana charging rate at a perfect stat, as a fraction. This stacks",
                     "with magicCapacityBonus, because manaChargePercentPerSecond is a percentage of your",
                     "already-boosted maximum — the two multiply into each other.",
                     "At 1.5 and level 100 — Proficiency 10: rate x2.5, a 200-mana caster goes 20 -> 125/sec.",
                     "                       Proficiency 1:  rate x1.15, that caster goes 20 -> 26.5/sec.",
                     "Base the rate on unboosted mana instead if you want this to stay linear.",
                     "Default: 1.5. Range: 0 to 20.")
            .defineInRange("magicChargeBonus", 1.5, 0.0, 20.0);

    public static final ForgeConfigSpec.DoubleValue MAGIC_GATE_BONUS = BUILDER
            .comment("Magic Mastery: bonus Gate Output and Efficiency at a perfect stat, as a fraction. These",
                     "decide cast speed and cost, so it is kept lower than the others.",
                     "At 0.75 and level 100, for a 10/10 gate casting Al Karum (1000 mana) —",
                     "  Proficiency 10: both become 18, cast 5.0s -> 2.8s, cost 500 -> 278 mana.",
                     "  Proficiency 1:  both become 11, cast 5.0s -> 4.55s, cost 500 -> 455 mana.",
                     "Default: 0.75. Range: 0 to 10.")
            .defineInRange("magicGateBonus", 0.75, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue COMBAT_DAMAGE_BONUS = BUILDER
            .comment("Combat Mastery: bonus weapon damage at a perfect stat, as a fraction. Lower than",
                     "strengthFistDamageBonus because weapons already hit hard.",
                     "At 0.9 and level 100 — Proficiency 10: +90%, a diamond sword's 7 deals 13.3 (19.95 crit).",
                     "                       Proficiency 1:  +9%, that sword deals 7.63 (11.4 crit).",
                     "Default: 0.9. Range: 0 to 20.")
            .defineInRange("combatDamageBonus", 0.9, 0.0, 20.0);

    public static final ForgeConfigSpec.DoubleValue COMBAT_ATTACK_SPEED_BONUS = BUILDER
            .comment("Combat Mastery: bonus attack speed while holding a weapon at a perfect stat, as a",
                     "fraction.",
                     "At 0.525 and level 100 — Proficiency 10: a diamond sword's 1.6 becomes 2.44 (0.41s).",
                     "                         Proficiency 1:  that sword becomes 1.684 (0.59s, vanilla 0.63s).",
                     "Default: 0.525. Range: 0 to 5.")
            .defineInRange("combatAttackSpeedBonus", 0.525, 0.0, 5.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_DAMAGE = BUILDER
            .comment("Player stats: stat progress awarded per point of damage taken or dealt. Half a heart is",
                     "1 damage. Default: 2.0. Range: 0 to 1000.")
            .defineInRange("statXpPerDamage", 2.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_HEAL = BUILDER
            .comment("Player stats: Tenacity progress awarded per point of health regenerated naturally.",
                     "Default: 3.0. Range: 0 to 1000.")
            .defineInRange("statXpPerHeal", 3.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_SHIELD_BLOCK = BUILDER
            .comment("Player stats: Fortitude progress awarded per point of damage blocked. Default: 3.0.",
                     "Range: 0 to 1000.")
            .defineInRange("statXpPerShieldBlock", 3.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_HAND_BREAK = BUILDER
            .comment("Player stats: Strength progress awarded per block broken bare-handed. Default: 2.0.",
                     "Range: 0 to 1000.")
            .defineInRange("statXpPerHandBreak", 2.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_SPRINT_BLOCK = BUILDER
            .comment("Player stats: Reflexes progress awarded per block sprinted. Default: 0.5.",
                     "Range: 0 to 1000.")
            .defineInRange("statXpPerSprintBlock", 0.5, 0.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_100_MANA_SPENT = BUILDER
            .comment("Player stats: Magic Mastery progress awarded per 100 mana spent casting. Default: 8.0.",
                     "Range: 0 to 1000.")
            .defineInRange("statXpPer100ManaSpent", 8.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue STAT_XP_PER_100_MANA_CHARGED = BUILDER
            .comment("Player stats: Magic Mastery progress awarded per 100 mana drawn in while charging.",
                     "Default: 3.0. Range: 0 to 1000.")
            .defineInRange("statXpPer100ManaCharged", 3.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.IntValue INSANITY_MAX_LEVEL = BUILDER
            .comment("Insanity: highest tier the effect can reach. Higher tiers act more often, hit harder,",
                     "and start costing you control of your own body. Default: 5. Range: 1 to 10.")
            .defineInRange("insanityMaxLevel", 5, 1, 10);

    public static final ForgeConfigSpec.IntValue INSANITY_DURATION_SECONDS = BUILDER
            .comment("Insanity: how long the effect lasts, refreshed every time it is raised.",
                     "Default: 300. Range: 10 to 86400.")
            .defineInRange("insanityDurationSeconds", 300, 10, 86400);

    public static final ForgeConfigSpec.IntValue INSANITY_EPISODE_BASE_SECONDS = BUILDER
            .comment("Insanity: rough spacing (seconds) between episodes at tier 1. Each tier shortens the gap,",
                     "so higher tiers afflict you far more often. Default: 30. Range: 1 to 600.")
            .defineInRange("insanityEpisodeBaseSeconds", 30, 1, 600);

    public static final ForgeConfigSpec.IntValue INSANITY_DAMAGE_TIER = BUILDER
            .comment("Insanity: the tier at which episodes start drawing blood. Damage scales with how far",
                     "past this tier you are, and like poison it can never drop you below half a heart.",
                     "Default: 3. Range: 1 to 10.")
            .defineInRange("insanityDamageTier", 3, 1, 10);

    public static final ForgeConfigSpec.IntValue INSANITY_CONTROL_LOSS_TIER = BUILDER
            .comment("Insanity: the tier at which episodes can wrest control of your body away from you",
                     "(the Bodily Disconnect flailing, without the blackout). Default: 4. Range: 1 to 10.")
            .defineInRange("insanityControlLossTier", 4, 1, 10);

    public static final ForgeConfigSpec.IntValue INSANITY_CONTROL_LOSS_CHANCE = BUILDER
            .comment("Insanity: percent chance an episode at or past insanityControlLossTier takes control.",
                     "Default: 35. Range: 0 to 100.")
            .defineInRange("insanityControlLossChance", 35, 0, 100);

    public static final ForgeConfigSpec.DoubleValue INSANITY_MIASMA_DRAW_PER_TIER = BUILDER
            .comment("Insanity: how much miasma you must draw out of chunks to gain one tier. Drawing from",
                     "clean chunks costs you nothing; this only counts miasma above the sickness threshold.",
                     "Default: 400. Range: 1 to 1000000.")
            .defineInRange("insanityMiasmaDrawPerTier", 400.0, 1.0, 1_000_000.0);

    public static final ForgeConfigSpec.DoubleValue INSANITY_FOOD_MEALS_PER_TIER = BUILDER
            .comment("Insanity: how many contaminated meals it takes to gain one tier. Default: 2.",
                     "Range: 1 to 1000.")
            .defineInRange("insanityFoodMealsPerTier", 2.0, 1.0, 1000.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConfigPlayer() {}
}
