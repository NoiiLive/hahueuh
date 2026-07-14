package net.noiilive.hahueuh;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ConfigMain {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SINGLE_AUTHORITY_HOLDER = BUILDER
            .comment("If true, only one player on the server may hold each authority at a time. For Return by",
                     "Death and Domain, this restricts the authority itself — granting either one to a player",
                     "automatically revokes it from whoever currently holds it. For Sloth and Greed, both of",
                     "which are also tied to a Witch Factor (see witchfactor commands), this instead restricts",
                     "the Witch Factor: multiple players may hold the Sloth/Greed authority itself at once, but",
                     "only one may have that authority's Witch Factor — granting a Witch Factor to a player",
                     "(explicitly, or by default when first granted the authority) automatically revokes it from",
                     "whoever currently holds it. Variants don't count as separate authorities: with this on,",
                     "there can only ever be one Greed Witch Factor holder at all, regardless of which Greed",
                     "variant (Lion's Heart, Cor Leonis, Echidna) they're set to — same for Sloth's variants.",
                     "Default: false.")
            .define("singleAuthorityHolder", false);

    public static final ModConfigSpec.BooleanValue COMPATIBILITY_ENABLED = BUILDER
            .comment("If true, Sloth and Greed users must earn 'compatibility' with their authority before",
                     "using it without drawbacks (blindness, hunger, nausea, and steady damage) — see",
                     "slothCompatibilityThreshold/greedCompatibilityThreshold in each authority's own config.",
                     "Set to false to let every Sloth/Greed holder use their authority freely, with no",
                     "compatibility tracking or penalty at all. Default: true.")
            .define("compatibilityEnabled", true);

    public static final ModConfigSpec.IntValue STARTING_COMPATIBILITY_MIN = BUILDER
            .comment("The Sloth/Greed compatibility score a player is randomly given the first time they're",
                     "granted that authority (see slothCompatibilityThreshold/greedCompatibilityThreshold) is",
                     "rolled between this and startingCompatibilityMax, inclusive. Only applies once, the very",
                     "first time — a player who already has a tracked score (even 0 from an explicit",
                     "'compatibility set') keeps it instead of being rerolled. Default: 0. Range: 0 to 1000000.")
            .defineInRange("startingCompatibilityMin", 0, 0, 1_000_000);

    public static final ModConfigSpec.IntValue STARTING_COMPATIBILITY_MAX = BUILDER
            .comment("See startingCompatibilityMin. Default: 100. Range: 0 to 1000000.")
            .defineInRange("startingCompatibilityMax", 100, 0, 1_000_000);

    public static final ModConfigSpec.DoubleValue WITCH_FACTOR_SPEED = BUILDER
            .comment("Movement speed modifier for a Witch Factor pathing toward its target — the same scale",
                     "as a player's own walk speed (1.0), so this should stay well below that to keep it slow.",
                     "Default: 1.0. Range: 0.01 to 2.0.")
            .defineInRange("witchFactorSpeed", 1.0, 0.01, 2.0);

    public static final ModConfigSpec.IntValue WITCH_FACTOR_RETARGET_SECONDS = BUILDER
            .comment("How often (in seconds) a Witch Factor re-scans every online player holding its",
                     "assigned authority and re-picks whichever one currently has the highest compatibility",
                     "score to path toward. Default: 5. Range: 1 to 3600.")
            .defineInRange("witchFactorRetargetSeconds", 5, 1, 3600);

    public static final ModConfigSpec.BooleanValue LOSE_WITCH_FACTOR_ON_DEATH = BUILDER
            .comment("If true, dying while holding a Sloth and/or Greed Witch Factor spawns a Witch Factor",
                     "entity (assigned to that sin) at the death location for each Witch Factor held, and",
                     "clears that Witch Factor from the player — they keep the underlying authority itself,",
                     "just not its Witch Factor, so someone else can go claim the one that spawned. Since the",
                     "Witch Factor is gone after the first death, later deaths with nothing left to lose do",
                     "nothing. Has no effect on a death that's rolled back (Return by Death/Domain), since the",
                     "player never actually died. Mainly intended for Hardcore worlds, where every death is",
                     "already final anyway. Default: false.")
            .define("loseWitchFactorOnDeath", false);

    public static final ModConfigSpec.BooleanValue MOB_WITCH_FACTORS_ENABLED = BUILDER
            .comment("If true, mobs (see mobWitchFactorEligibleEntities) become valid Witch Factor targets:",
                     "a Witch Factor may seek out and be absorbed by an eligible nearby mob instead of a",
                     "player, if that mob turns out more compatible. The mob rolls a starting compatibility",
                     "score the same way a player does (startingCompatibilityMin/Max) the moment it's first",
                     "considered. Default: true.")
            .define("mobWitchFactorsEnabled", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_WITCH_FACTOR_ELIGIBLE_ENTITIES = BUILDER
            .comment("Entity types eligible to seek/hold a Witch Factor when mobWitchFactorsEnabled is true.",
                     "Use full entity type ids, e.g. \"minecraft:villager\". Default: zombies, villagers and",
                     "illagers.")
            .defineList("mobWitchFactorEligibleEntities",
                    List.of("minecraft:villager", "minecraft:zombie_villager", "minecraft:pillager", "minecraft:vindicator",
                            "minecraft:evoker", "minecraft:zombie"),
                    () -> "minecraft:villager",
                    obj -> obj instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec.BooleanValue MOB_WITCH_FACTOR_NATURAL_SPAWN_ENABLED = BUILDER
            .comment("If true (and mobWitchFactorsEnabled is also true), an eligible mob may naturally spawn",
                     "already holding a Witch Factor — see mobWitchFactorNaturalSpawnChance. Default: true.")
            .define("mobWitchFactorNaturalSpawnEnabled", true);

    public static final ModConfigSpec.DoubleValue MOB_WITCH_FACTOR_NATURAL_SPAWN_CHANCE = BUILDER
            .comment("Percent chance an eligible mob naturally spawns already holding a Witch Factor. When",
                     "singleAuthorityHolder is on, a sin (Sloth or Greed) already held by any player, mob, or",
                     "still-wandering Witch Factor entity anywhere is skipped — if both are taken, nothing",
                     "spawns; if one is free, that one is used; if both are free, one is picked at random.",
                     "When singleAuthorityHolder is off, this check is skipped and either sin may be picked",
                     "regardless of who else already holds one. Default: 3. Range: 0 to 100.")
            .defineInRange("mobWitchFactorNaturalSpawnChance", 3.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue MOB_WITCH_FACTOR_NATURAL_SPAWN_MIN_Y = BUILDER
            .comment("An eligible mob only rolls to naturally spawn with a Witch Factor (see",
                     "mobWitchFactorNaturalSpawnChance) if it spawns at or above this Y level. Exists so a",
                     "server's only copy of a sin's Witch Factor, under singleAuthorityHolder, can't end up",
                     "buried deep in a cave where nobody will ever realistically find it. Default: 63 (sea",
                     "level). Range: -64 to 320.")
            .defineInRange("mobWitchFactorNaturalSpawnMinY", 63, -64, 320);

    public static final ModConfigSpec.DoubleValue SAGE_CANDIDATE_CHANCE = BUILDER
            .comment("Percent chance a player is rolled as a Sage Candidate the first time they're ever seen",
                     "(same idempotent, once-per-player timing as the starting compatibility roll — see",
                     "startingCompatibilityMin/Max). A Sage Candidate can foster more than one Witch Factor at",
                     "once; everyone else is capped to holding a single Witch Factor at a time (a second Witch",
                     "Factor, wandering or via the witchfactor commands, simply won't target/grant to them while",
                     "they already hold a different one) — and since a Witch Factor also grants its authority,",
                     "this in practice caps most players to one authority too, by default. Use",
                     "/rezero sagecandidate <player> <true/false> to set it directly. Default: 10. Range: 0 to 100.")
            .defineInRange("sageCandidateChance", 10.0, 0.0, 100.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigMain() {}
}
