package net.noiilive.hahueuh;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigMagic {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue OD_LIFESPAN_MULTIPLIER = BUILDER
            .comment("A player's max Od is their Lifespan multiplied by this. Max Mana is the same as max",
                     "Od. Default: 2. Range: 1 to 100.")
            .defineInRange("odLifespanMultiplier", 2, 1, 100);

    public static final ForgeConfigSpec.IntValue GATE_OUTPUT_MAX = BUILDER
            .comment("Highest value a player's Gate Output can roll (rolled 1 to this, inclusive). Output is",
                     "per-tick mana throughput: it sets how fast a spell charges, and a spell demanding more",
                     "per tick than this can strain or outright fail the cast. Raising it makes strong gates",
                     "possible; the roll is uniform, so it also raises the average. Default: 10. Range: 1 to 1000.")
            .defineInRange("gateOutputMax", 10, 1, 1000);

    public static final ForgeConfigSpec.IntValue GATE_EFFICIENCY_MAX = BUILDER
            .comment("Highest value a player's Gate Efficiency can roll (rolled 1 to this, inclusive).",
                     "Efficiency divides a spell's mana cost relative to castEfficiencyBaseline, so a value",
                     "above the baseline spends less than the raw cost. Default: 10. Range: 1 to 1000.")
            .defineInRange("gateEfficiencyMax", 10, 1, 1000);

    public static final ForgeConfigSpec.DoubleValue MANA_CHARGE_PERCENT_PER_SECOND = BUILDER
            .comment("Percent of max Mana gained per second while charging — either by holding the",
                     "charge-mana key (default Left Alt), or passively (no key needed) for a No Release",
                     "Defective Gate. Default: 10. Range: 0.01 to 1000.")
            .defineInRange("manaChargePercentPerSecond", 10.0, 0.01, 1000.0);

    public static final ForgeConfigSpec.IntValue GATE_STRAIN_DAMAGED = BUILDER
            .comment("Gate Strain (0-100) at or above which a player's Gate status auto-transitions to",
                     "Damaged — only while their current status is Open or Damaged; other statuses are",
                     "left alone regardless of Strain. Default: 75. Range: 1 to 100.")
            .defineInRange("gateStrainDamaged", 75, 1, 100);

    public static final ForgeConfigSpec.IntValue GATE_STRAIN_DESTROYED = BUILDER
            .comment("Gate Strain (0-100) at or above which a player's Gate status auto-transitions to",
                     "Destroyed (unsalvageable — never reverts even if Strain drops back down). Same",
                     "Open/Damaged-only restriction as gateStrainDamaged. Default: 100. Range: 1 to 100.")
            .defineInRange("gateStrainDestroyed", 100, 1, 100);

    public static final ForgeConfigSpec.IntValue OVERCHARGE_FREE_HEADROOM_PERCENT = BUILDER
            .comment("Charging mana is allowed to overcharge up to double (200%) a player's normal max mana.",
                     "This much overcharge, as a percentage of max mana above 100%, can be held with NO Gate",
                     "Strain at all — so a small or moderate overcharge is safe to sit on. Only overcharge",
                     "beyond this headroom strains the gate. Default: 50 (i.e. up to 150% is free).",
                     "Range: 0 to 100.")
            .defineInRange("overchargeFreeHeadroomPercent", 50, 0, 100);

    public static final ForgeConfigSpec.IntValue OVERCHARGE_STRAIN_PER_TIER_PER_SECOND = BUILDER
            .comment("Gate Strain added per second, per 10% overcharge tier BEYOND the free headroom",
                     "(overchargeFreeHeadroomPercent), while an overcharge is held. There is no longer any",
                     "one-time strain for reaching a tier — only this gentle sustained drip. E.g. at the",
                     "defaults (headroom 50, rate 1): 150% is free, 180% bleeds 3 Strain/sec, 200% bleeds 5.",
                     "Default: 1. Range: 0 to 100.")
            .defineInRange("overchargeStrainPerTierPerSecond", 1, 0, 100);

    public static final ForgeConfigSpec.IntValue CHUNK_AMBIENT_MANA_CAP = BUILDER
            .comment("Maximum ambient mana held in each chunk — the pool players draw from when charging",
                     "(the mana 'in the air'). A fresh/undrained chunk starts full at this cap; drain a",
                     "chunk dry and you can't charge there until it replenishes, so you must move on.",
                     "Default: 5000. Range: 1 to 100000000.")
            .defineInRange("chunkAmbientManaCap", 5000, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue CHUNK_REPLENISH_DAYS = BUILDER
            .comment("In-game days for a fully-drained chunk to replenish its ambient mana back to the",
                     "cap (chunkAmbientManaCap). Replenishment is linear and computed lazily, so chunks",
                     "refill even while unloaded — measured in game-time ticks (24000 per day); time the",
                     "server spends offline does not count. Default: 3. Range: 0.01 to 365.")
            .defineInRange("chunkReplenishDays", 3.0, 0.01, 365.0);

    public static final ForgeConfigSpec.IntValue MIASMA_CAP = BUILDER
            .comment("Maximum miasma (corruption) a chunk can hold. Raised by sin (Sloth/Greed) ability",
                     "use in that chunk; decays back toward 0 over time (miasmaDecayDays) when unused.",
                     "Default: 500. Range: 1 to 100000000.")
            .defineInRange("miasmaCap", 500, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue MIASMA_EFFECT_THRESHOLD_PERCENT = BUILDER
            .comment("Miasma concentration, as a percentage of miasmaCap, at/above which a chunk's",
                     "effects kick in: gate-bearing players standing there sicken (and go 'mad'), and",
                     "food harvested/killed there is contaminated. Below this, miasma is inert.",
                     "Default: 30. Range: 1 to 100.")
            .defineInRange("miasmaEffectThresholdPercent", 30, 1, 100);

    public static final ForgeConfigSpec.DoubleValue MIASMA_DECAY_DAYS = BUILDER
            .comment("In-game days for a fully-saturated chunk's miasma to decay from the cap back to 0",
                     "with no further ability use. Linear and computed lazily (fades even while the chunk",
                     "is unloaded; offline server time does not count). Default: 3. Range: 0.01 to 365.")
            .defineInRange("miasmaDecayDays", 3.0, 0.01, 365.0);

    public static final ForgeConfigSpec.IntValue MIASMA_PER_SINGLE_USE = BUILDER
            .comment("Miasma added to a chunk each time a single-use sin ability is activated there",
                     "(by a player, a Finger recipient, or an authority-wielding mob). Default: 1.",
                     "Range: 0 to 100000000.")
            .defineInRange("miasmaPerSingleUse", 1, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue MIASMA_PER_TOGGLE_SECOND = BUILDER
            .comment("Miasma added to a chunk each second a sustained/toggle sin ability (Lion's Heart,",
                     "Material Phase, summoned Unseen Hands, Visions, Shifts, Book of Wisdom, etc.) is",
                     "active there. Default: 1. Range: 0 to 100000000.")
            .defineInRange("miasmaPerToggleSecond", 1, 0, 100_000_000);


    public static final ForgeConfigSpec.DoubleValue CAST_EFFICIENCY_BASELINE = BUILDER
            .comment("Baseline Gate Efficiency for spell mana cost. A spell's raw mana cost is scaled by",
                     "(this / the caster's Efficiency): a caster at exactly this Efficiency pays the raw",
                     "cost, higher Efficiency pays less, lower Efficiency pays more (e.g. at baseline 5,",
                     "Efficiency 10 halves the cost and Efficiency 2 makes it 2.5x). Default: 5. Range: 1 to 10.")
            .defineInRange("castEfficiencyBaseline", 5.0, 1.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue CAST_FAIL_RATIO = BUILDER
            .comment("A spell fails outright if its manaPerTick divided by the caster's Gate Output is at or",
                     "above this ratio — i.e. the spell demands far more flow per tick than the gate can give.",
                     "A failed cast applies the fatigue debuff and strain below instead of casting. Between",
                     "1 and this ratio, the cast still succeeds but strains the gate. Default: 3. Range: 1 to 100.")
            .defineInRange("castFailRatio", 3.0, 1.0, 100.0);

    public static final ForgeConfigSpec.IntValue CAST_FAIL_FATIGUE_SECONDS = BUILDER
            .comment("Duration, in seconds, of the heavy Fatigue debuff (Weakness + Mining Fatigue) applied",
                     "when a cast fails outright. Default: 10. Range: 0 to 3600.")
            .defineInRange("castFailFatigueSeconds", 10, 0, 3600);

    public static final ForgeConfigSpec.IntValue CAST_FAIL_FATIGUE_AMPLIFIER = BUILDER
            .comment("Amplifier (0 = level I) of the Weakness + Mining Fatigue applied on a failed cast.",
                     "Default: 2 (level III). Range: 0 to 255.")
            .defineInRange("castFailFatigueAmplifier", 2, 0, 255);

    public static final ForgeConfigSpec.IntValue CAST_FAIL_STRAIN = BUILDER
            .comment("Gate Strain added when a cast fails outright (the difference was too great). Default: 15.",
                     "Range: 0 to 100.")
            .defineInRange("castFailStrain", 15, 0, 100);

    public static final ForgeConfigSpec.IntValue CAST_STRAIN_PER_SECOND = BUILDER
            .comment("Gate Strain added per second while sustaining a cast whose manaPerTick exceeds the",
                     "caster's Gate Output (the gate is queuing energy it can't release fast enough).",
                     "Default: 2. Range: 0 to 100.")
            .defineInRange("castStrainPerSecond", 2, 0, 100);


    public static final ForgeConfigSpec.DoubleValue SPELL_HEAT_PER_MANA_PER_TICK = BUILDER
            .comment("Heat a cast adds to the caster's Gate = the spell's raw manaPerTick multiplied by",
                     "this. Heat's cap is the caster's max Mana, and it resets every in-game day. Casting",
                     "past the cap doesn't add more heat — instead it strains the Gate by exactly the",
                     "amount that would have overflowed the cap. E.g. on a 100-max gate, a 25-heat spell",
                     "(manaPerTick 5 at this default) can be cast 4 times heat-free; a 5th cast (already at",
                     "the 100 cap) strains the gate by the full 25; casting at 90 heat instead strains by",
                     "just the 15 that would have overflowed. Default: 5. Range: 0.01 to 1000.")
            .defineInRange("spellHeatPerManaPerTick", 5.0, 0.01, 1000.0);


    public static final ForgeConfigSpec.BooleanValue CRIPPLED_ENABLED = BUILDER
            .comment("Whether spending Od to 0 inflicts the permanent, irreversible crippled ('withered')",
                     "state at all. If disabled, spending Od to 0 instead just refills Od (as though the",
                     "player had died) and locks out every magic skill for crippledMagicLockoutMinutes —",
                     "a much softer punishment. Default: true.")
            .define("crippledEnabled", true);

    public static final ForgeConfigSpec.IntValue CRIPPLED_MAGIC_LOCKOUT_MINUTES = BUILDER
            .comment("When crippledEnabled is false, spending Od to 0 locks every magic skill's cooldown for",
                     "this many minutes instead of permanently crippling the player. Default: 20.",
                     "Range: 0 to 1440.")
            .defineInRange("crippledMagicLockoutMinutes", 20, 0, 1440);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConfigMagic() {}
}
