package net.noiilive.hahueuh;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigMagicYin {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SHAMAK_RADIUS = BUILDER
            .comment("Shamak: radius (blocks) of the squid-ink cloud, which expands outward from the caster",
                     "to this radius. Every entity caught inside is struck with Sensory Deprivation.",
                     "Default: 16. Range: 1 to 128.")
            .defineInRange("shamakRadius", 16, 1, 128);

    public static final ForgeConfigSpec.IntValue SHAMAK_COOLDOWN_SECONDS = BUILDER
            .comment("Shamak: cooldown in seconds after a successful cast. Default: 15. Range: 0 to 3600.")
            .defineInRange("shamakCooldownSeconds", 15, 0, 3600);

    public static final ForgeConfigSpec.IntValue SHAMAK_TOTAL_MANA = BUILDER
            .comment("Shamak: total raw mana the spell costs to cast (before Efficiency scaling). Default: 100.",
                     "Range: 1 to 100000000.")
            .defineInRange("shamakTotalMana", 100, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue SHAMAK_MANA_PER_TICK = BUILDER
            .comment("Shamak: mana-per-tick draw rate. Compared against the caster's Gate Output to decide",
                     "cast speed, strain, or outright failure (see castFailRatio in magic_main). Default: 3.",
                     "Range: 1 to 100000000.")
            .defineInRange("shamakManaPerTick", 3, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue SHAMAK_EFFECT_SECONDS = BUILDER
            .comment("Shamak: duration in seconds of the Sensory Deprivation effect applied to entities caught",
                     "in the ink cloud. Default: 10. Range: 1 to 3600.")
            .defineInRange("shamakEffectSeconds", 10, 1, 3600);


    public static final ForgeConfigSpec.IntValue EL_SHAMAK_RADIUS = BUILDER
            .comment("El Shamak: radius (blocks) of the Yin distortion cloud. Every entity caught inside",
                     "suffers Bodily Disconnect (their body acts on its own). Default: 10. Range: 1 to 128.")
            .defineInRange("elShamakRadius", 10, 1, 128);

    public static final ForgeConfigSpec.IntValue EL_SHAMAK_COOLDOWN_SECONDS = BUILDER
            .comment("El Shamak: cooldown in seconds after a successful cast. Default: 20. Range: 0 to 3600.")
            .defineInRange("elShamakCooldownSeconds", 20, 0, 3600);

    public static final ForgeConfigSpec.IntValue EL_SHAMAK_TOTAL_MANA = BUILDER
            .comment("El Shamak: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 300. Range: 1 to 100000000.")
            .defineInRange("elShamakTotalMana", 300, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EL_SHAMAK_MANA_PER_TICK = BUILDER
            .comment("El Shamak: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 5.",
                     "Range: 1 to 100000000.")
            .defineInRange("elShamakManaPerTick", 5, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EL_SHAMAK_EFFECT_SECONDS = BUILDER
            .comment("El Shamak: duration in seconds of the Bodily Disconnect effect applied to entities",
                     "caught in the cloud. Default: 8. Range: 1 to 3600.")
            .defineInRange("elShamakEffectSeconds", 8, 1, 3600);


    public static final ForgeConfigSpec.DoubleValue UL_SHAMAK_PULL_RADIUS = BUILDER
            .comment("Ul Shamak: radius (blocks) of the suction zone — everything within is dragged toward",
                     "the black hole's centre. Default: 12. Range: 1 to 128.")
            .defineInRange("ulShamakPullRadius", 12.0, 1.0, 128.0);

    public static final ForgeConfigSpec.DoubleValue UL_SHAMAK_EVENT_HORIZON_RADIUS = BUILDER
            .comment("Ul Shamak: radius (blocks) of the event horizon — entities touching this core take",
                     "damage, and sucked-in blocks/items are consumed here. Default: 3. Range: 0.5 to 64.")
            .defineInRange("ulShamakEventHorizonRadius", 3.0, 0.5, 64.0);

    public static final ForgeConfigSpec.IntValue UL_SHAMAK_COOLDOWN_SECONDS = BUILDER
            .comment("Ul Shamak: cooldown in seconds after a successful cast. Default: 60. Range: 0 to 3600.")
            .defineInRange("ulShamakCooldownSeconds", 60, 0, 3600);

    public static final ForgeConfigSpec.IntValue UL_SHAMAK_TOTAL_MANA = BUILDER
            .comment("Ul Shamak: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 500. Range: 1 to 100000000.")
            .defineInRange("ulShamakTotalMana", 500, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue UL_SHAMAK_MANA_PER_TICK = BUILDER
            .comment("Ul Shamak: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 7.",
                     "Range: 1 to 100000000.")
            .defineInRange("ulShamakManaPerTick", 7, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue UL_SHAMAK_DURATION_SECONDS = BUILDER
            .comment("Ul Shamak: how long the black hole persists once formed, before collapsing.",
                     "Default: 6. Range: 1 to 300.")
            .defineInRange("ulShamakDurationSeconds", 6, 1, 300);

    public static final ForgeConfigSpec.DoubleValue UL_SHAMAK_CORE_DAMAGE = BUILDER
            .comment("Ul Shamak: magic damage dealt to entities in the event horizon, every 10 ticks",
                     "(half-second). Default: 3. Range: 0 to 1000.")
            .defineInRange("ulShamakCoreDamage", 3.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.IntValue UL_SHAMAK_BLOCKS_PER_TICK = BUILDER
            .comment("Ul Shamak: how many random breakable blocks the black hole tears loose (as falling",
                     "blocks, then sucked in) per tick from within the pull radius. 0 disables block",
                     "suction entirely. Also requires the rezeroBlockDestruction game rule. Default: 6.",
                     "Range: 0 to 64.")
            .defineInRange("ulShamakBlocksPerTick", 6, 0, 64);


    public static final ForgeConfigSpec.DoubleValue AL_SHAMAK_RANGE = BUILDER
            .comment("Al Shamak: max line-of-sight distance (blocks) to the entity you banish into the",
                     "pocket dimension. Default: 7. Range: 1 to 64.")
            .defineInRange("alShamakRange", 7.0, 1.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue AL_SHAMAK_AOE_RADIUS = BUILDER
            .comment("Al Shamak: any OTHER creature within this radius (blocks) of the targeted entity is",
                     "banished into the SAME shared pocket-dimension room alongside it. Default: 10.",
                     "Range: 0 to 64.")
            .defineInRange("alShamakAoeRadius", 10.0, 0.0, 64.0);

    public static final ForgeConfigSpec.IntValue AL_SHAMAK_COOLDOWN_SECONDS = BUILDER
            .comment("Al Shamak: cooldown in seconds after a successful cast. Default: 120. Range: 0 to 3600.")
            .defineInRange("alShamakCooldownSeconds", 120, 0, 3600);

    public static final ForgeConfigSpec.IntValue AL_SHAMAK_TOTAL_MANA = BUILDER
            .comment("Al Shamak: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 800. Range: 1 to 100000000.")
            .defineInRange("alShamakTotalMana", 1000, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue AL_SHAMAK_MANA_PER_TICK = BUILDER
            .comment("Al Shamak: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 9.",
                     "Range: 1 to 100000000.")
            .defineInRange("alShamakManaPerTick", 9, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue AL_SHAMAK_BANISH_SECONDS = BUILDER
            .comment("Al Shamak: how long a banished entity is held in the pocket dimension before being",
                     "returned to where it was taken from. Default: 15. Range: 1 to 3600.")
            .defineInRange("alShamakBanishSeconds", 15, 1, 3600);

    public static final ForgeConfigSpec.IntValue AL_SHAMAK_STORE_COOLDOWN_SECONDS = BUILDER
            .comment("Al Shamak: the SHORT cooldown applied when a spell finishes charging into storage",
                     "(as opposed to the full alShamakCooldownSeconds applied when you actually release a",
                     "banish or a stored spell). Default: 5. Range: 0 to 3600.")
            .defineInRange("alShamakStoreCooldownSeconds", 5, 0, 3600);


    public static final ForgeConfigSpec.IntValue MINYA_COOLDOWN_SECONDS = BUILDER
            .comment("Minya: cooldown in seconds after a cast. Default: 8. Range: 0 to 3600.")
            .defineInRange("minyaCooldownSeconds", 8, 0, 3600);

    public static final ForgeConfigSpec.IntValue MINYA_TOTAL_MANA = BUILDER
            .comment("Minya: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 150. Range: 1 to 100000000.")
            .defineInRange("minyaTotalMana", 150, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue MINYA_MANA_PER_TICK = BUILDER
            .comment("Minya: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 3.",
                     "Range: 1 to 100000000.")
            .defineInRange("minyaManaPerTick", 3, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue MINYA_DAMAGE = BUILDER
            .comment("Minya: armor-piercing \"Magic\" damage each of the 3 crystalline stakes deals on hit.",
                     "Default: 6.0. Range: 0 to 1000.")
            .defineInRange("minyaDamage", 6.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.IntValue MINYA_CRYSTALLIZE_SECONDS = BUILDER
            .comment("Minya: how long a struck target is Crystallized (rooted in place — no movement or",
                     "jumping) before it shatters. Default: 2. Range: 1 to 3600.")
            .defineInRange("minyaCrystallizeSeconds", 2, 1, 3600);

    public static final ForgeConfigSpec.DoubleValue MINYA_SHATTER_PERCENT = BUILDER
            .comment("Minya: Shatter burst dealt when Crystallized ends, as a percent of the target's",
                     "CURRENT health at that moment. Default: 20.0. Range: 0 to 100.")
            .defineInRange("minyaShatterPercent", 20.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MINYA_SHARD_DAMAGE_PERCENT = BUILDER
            .comment("Minya: damage a fragmentation shard deals, as a percent of a full stake's damage.",
                     "A stake that misses or is blocked splits into 3 of these. Default: 25.0. Range: 0 to 100.")
            .defineInRange("minyaShardDamagePercent", 25.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MINYA_SHARD_HOMING_RANGE = BUILDER
            .comment("Minya: radius (blocks) a fragmentation shard scans for a new nearest target to home",
                     "toward. Default: 5.0. Range: 1 to 64.")
            .defineInRange("minyaShardHomingRange", 5.0, 1.0, 64.0);


    public static final ForgeConfigSpec.IntValue EL_MINYA_COOLDOWN_SECONDS = BUILDER
            .comment("El Minya: cooldown in seconds after a cast. Default: 15. Range: 0 to 3600.")
            .defineInRange("elMinyaCooldownSeconds", 15, 0, 3600);

    public static final ForgeConfigSpec.IntValue EL_MINYA_TOTAL_MANA = BUILDER
            .comment("El Minya: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 300. Range: 1 to 100000000.")
            .defineInRange("elMinyaTotalMana", 300, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EL_MINYA_MANA_PER_TICK = BUILDER
            .comment("El Minya: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 5.",
                     "Range: 1 to 100000000.")
            .defineInRange("elMinyaManaPerTick", 5, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EL_MINYA_STAKE_COUNT = BUILDER
            .comment("El Minya: how many crystalline stakes are fired in the fan. Default: 15. Range: 1 to 64.")
            .defineInRange("elMinyaStakeCount", 15, 1, 64);

    public static final ForgeConfigSpec.DoubleValue EL_MINYA_DAMAGE = BUILDER
            .comment("El Minya: armor-piercing damage each stake deals on hit (there are many, so this is",
                     "lower than Minya's). Default: 4.0. Range: 0 to 1000.")
            .defineInRange("elMinyaDamage", 4.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.IntValue EL_MINYA_CHAIN_THRESHOLD = BUILDER
            .comment("El Minya: how many stakes must strike the SAME target within the chain window to",
                     "trigger an instant chain-reaction detonation. Default: 3. Range: 2 to 64.")
            .defineInRange("elMinyaChainThreshold", 3, 2, 64);

    public static final ForgeConfigSpec.DoubleValue EL_MINYA_CHAIN_WINDOW_SECONDS = BUILDER
            .comment("El Minya: the rolling time window (seconds) within which the chain-threshold hits",
                     "must land on one target to detonate. Default: 1.0. Range: 0.1 to 30.")
            .defineInRange("elMinyaChainWindowSeconds", 1.0, 0.1, 30.0);

    public static final ForgeConfigSpec.DoubleValue EL_MINYA_AOE_RADIUS = BUILDER
            .comment("El Minya: radius (blocks) of the chain-reaction detonation's area damage.",
                     "Default: 4.0. Range: 0 to 64.")
            .defineInRange("elMinyaAoeRadius", 4.0, 0.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue EL_MINYA_AOE_DAMAGE = BUILDER
            .comment("El Minya: armor-piercing area damage dealt to everything caught in a chain-reaction",
                     "detonation. Default: 8.0. Range: 0 to 1000.")
            .defineInRange("elMinyaAoeDamage", 8.0, 0.0, 1000.0);


    public static final ForgeConfigSpec.IntValue UL_MINYA_COOLDOWN_SECONDS = BUILDER
            .comment("Ul Minya: cooldown in seconds after a cast. Default: 60. Range: 0 to 3600.")
            .defineInRange("ulMinyaCooldownSeconds", 60, 0, 3600);

    public static final ForgeConfigSpec.IntValue UL_MINYA_TOTAL_MANA = BUILDER
            .comment("Ul Minya: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 1000. Range: 1 to 100000000.")
            .defineInRange("ulMinyaTotalMana", 1000, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue UL_MINYA_MANA_PER_TICK = BUILDER
            .comment("Ul Minya: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 8.",
                     "Range: 1 to 100000000.")
            .defineInRange("ulMinyaManaPerTick", 8, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue UL_MINYA_RANGE = BUILDER
            .comment("Ul Minya: max line-of-sight distance (blocks) to lock the ring onto a target.",
                     "Default: 24. Range: 1 to 64.")
            .defineInRange("ulMinyaRange", 24.0, 1.0, 64.0);

    public static final ForgeConfigSpec.IntValue UL_MINYA_BIND_SECONDS = BUILDER
            .comment("Ul Minya: how long the target is Leashed inside the ring before the execution beam",
                     "strikes. Default: 3. Range: 1 to 60.")
            .defineInRange("ulMinyaBindSeconds", 3, 1, 60);

    public static final ForgeConfigSpec.DoubleValue UL_MINYA_RING_SCALE = BUILDER
            .comment("Ul Minya: the mana ring's diameter is the target's own hitbox width times this —",
                     "so it scales automatically to fit snugly around whatever it's cast on (a narrow",
                     "target like an Enderman gets a small ring, a wide one gets a big ring) instead of",
                     "one fixed size for everything. Purely visual; the Leash pins the target to the",
                     "ring's centre regardless of size. Default: 1.6. Range: 1.0 to 6.0.")
            .defineInRange("ulMinyaRingScale", 1.6, 1.0, 6.0);

    public static final ForgeConfigSpec.DoubleValue UL_MINYA_DAMAGE = BUILDER
            .comment("Ul Minya: flat damage the execution beam deals when it strikes. Default: 40.0.",
                     "Range: 0 to 1000.")
            .defineInRange("ulMinyaDamage", 40.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.IntValue AL_KARUM_COOLDOWN_SECONDS = BUILDER
            .comment("Al Karum: cooldown in seconds after a successful cast. Default: 90. Range: 0 to 3600.")
            .defineInRange("alKarumCooldownSeconds", 90, 0, 3600);

    public static final ForgeConfigSpec.IntValue AL_KARUM_TOTAL_MANA = BUILDER
            .comment("Al Karum: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 1000. Range: 1 to 100000000.")
            .defineInRange("alKarumTotalMana", 1000, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue AL_KARUM_MANA_PER_TICK = BUILDER
            .comment("Al Karum: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 8.",
                     "Range: 1 to 100000000.")
            .defineInRange("alKarumManaPerTick", 8, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue AL_KARUM_PULL_RADIUS = BUILDER
            .comment("Al Karum: radius (blocks) of the suction zone — everything within is dragged toward",
                     "the black hole's centre. Twice Ul Shamak's by default. Default: 24. Range: 1 to 128.")
            .defineInRange("alKarumPullRadius", 24.0, 1.0, 128.0);

    public static final ForgeConfigSpec.DoubleValue AL_KARUM_EVENT_HORIZON_RADIUS = BUILDER
            .comment("Al Karum: radius (blocks) of the event horizon — entities touching this core take",
                     "damage, and sucked-in blocks/items are consumed here. Twice Ul Shamak's by default.",
                     "Default: 6. Range: 0.5 to 64.")
            .defineInRange("alKarumEventHorizonRadius", 6.0, 0.5, 64.0);

    public static final ForgeConfigSpec.IntValue AL_KARUM_DURATION_SECONDS = BUILDER
            .comment("Al Karum: how long the black hole persists once formed, before collapsing.",
                     "Default: 6. Range: 1 to 300.")
            .defineInRange("alKarumDurationSeconds", 6, 1, 300);

    public static final ForgeConfigSpec.DoubleValue AL_KARUM_CORE_DAMAGE = BUILDER
            .comment("Al Karum: magic damage dealt to entities in the event horizon, every 10 ticks",
                     "(half-second). Twice Ul Shamak's by default. Default: 6. Range: 0 to 1000.")
            .defineInRange("alKarumCoreDamage", 6.0, 0.0, 1000.0);

    public static final ForgeConfigSpec.IntValue AL_KARUM_BLOCKS_PER_TICK = BUILDER
            .comment("Al Karum: how many random breakable blocks the black hole tears loose (as falling",
                     "blocks, then sucked in) per tick from within the pull radius. 0 disables block",
                     "suction entirely. Also requires the rezeroBlockDestruction game rule. Twice Ul Shamak's",
                     "by default. Default: 12. Range: 0 to 64.")
            .defineInRange("alKarumBlocksPerTick", 12, 0, 64);

    public static final ForgeConfigSpec.IntValue MURAK_COOLDOWN_SECONDS = BUILDER
            .comment("Murak: cooldown in seconds after a cast. Default: 10. Range: 0 to 3600.")
            .defineInRange("murakCooldownSeconds", 10, 0, 3600);

    public static final ForgeConfigSpec.IntValue MURAK_TOTAL_MANA = BUILDER
            .comment("Murak: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 200. Range: 1 to 100000000.")
            .defineInRange("murakTotalMana", 200, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue MURAK_MANA_PER_TICK = BUILDER
            .comment("Murak: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 3.",
                     "Range: 1 to 100000000.")
            .defineInRange("murakManaPerTick", 3, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue MURAK_RANGE = BUILDER
            .comment("Murak: max line-of-sight distance (blocks) to pick a target. Sneak while casting to",
                     "target yourself instead. Default: 24. Range: 1 to 64.")
            .defineInRange("murakRange", 24.0, 1.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue MURAK_GRAVITY = BUILDER
            .comment("Murak: the gravity value Reduced Gravity sets on the target. Vanilla gravity is 0.08,",
                     "so lower means floatier (higher jumps, slower falls). Default: 0.01. Range: 0.001 to 0.08.")
            .defineInRange("murakGravity", 0.01, 0.001, 0.08);

    public static final ForgeConfigSpec.DoubleValue MURAK_FLIGHT_IMPULSE = BUILDER
            .comment("Murak flight: how much velocity a single movement input adds per tick. This is",
                     "deliberately large — you get shoved in a direction rather than steered. The push",
                     "follows where you are looking, so pitching up while holding forward carries you",
                     "upward as well. Default: 0.025. Range: 0.01 to 1.0.")
            .defineInRange("murakFlightImpulse", 0.025, 0.01, 1.0);

    public static final ForgeConfigSpec.DoubleValue MURAK_FLIGHT_DRAG = BUILDER
            .comment("Murak flight: per-tick momentum retention. Closer to 1.0 is more slippery — you keep",
                     "coasting long after you stop pressing anything. Must stay below 1.0 so you do",
                     "eventually slow, but it never brings you to a full stop. Default: 0.985.",
                     "Range: 0.9 to 0.999.")
            .defineInRange("murakFlightDrag", 0.985, 0.9, 0.999);

    public static final ForgeConfigSpec.DoubleValue MURAK_GUST_STRENGTH = BUILDER
            .comment("Murak flight: strength of an occasional gust — a single soft shove in a random",
                     "direction, applied once every few seconds rather than continuously. 0 disables",
                     "gusts entirely. Default: 0.25. Range: 0 to 1.0.")
            .defineInRange("murakGustStrength", 0.25, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue MURAK_GUST_INTERVAL_SECONDS = BUILDER
            .comment("Murak flight: average seconds between gusts. The actual gap is randomised around",
                     "this, so they never land on a predictable beat. Default: 2. Range: 1 to 120.")
            .defineInRange("murakGustIntervalSeconds", 2, 1, 120);

    public static final ForgeConfigSpec.DoubleValue MURAK_FLIGHT_MAX_SPEED = BUILDER
            .comment("Murak flight: hard cap on speed in blocks per tick. Kept well under the server's",
                     "movement-validation threshold so gusts can not get you kicked. Default: 0.4.",
                     "Range: 0.1 to 5.0.")
            .defineInRange("murakFlightMaxSpeed", 0.4, 0.1, 5.0);

    public static final ForgeConfigSpec.IntValue VITA_COOLDOWN_SECONDS = BUILDER
            .comment("Vita: cooldown in seconds after a cast. Default: 10. Range: 0 to 3600.")
            .defineInRange("vitaCooldownSeconds", 10, 0, 3600);

    public static final ForgeConfigSpec.IntValue VITA_TOTAL_MANA = BUILDER
            .comment("Vita: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 200. Range: 1 to 100000000.")
            .defineInRange("vitaTotalMana", 200, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue VITA_MANA_PER_TICK = BUILDER
            .comment("Vita: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 3.",
                     "Range: 1 to 100000000.")
            .defineInRange("vitaManaPerTick", 3, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue VITA_RANGE = BUILDER
            .comment("Vita: max line-of-sight distance (blocks) to pick a target. Sneak while casting to",
                     "target yourself instead. Default: 24. Range: 1 to 64.")
            .defineInRange("vitaRange", 24.0, 1.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue VITA_GRAVITY_MULTIPLIER = BUILDER
            .comment("Vita: gravity multiplier applied while Increased Gravity is active at level 1 (the",
                     "tier Vita itself applies — see elVitaGravityMultiplier for the level 2 tier El Vita",
                     "applies). Vanilla gravity is 0.08, so 1.5 makes the target noticeably heavier —",
                     "shorter jumps, faster falls. Default: 1.5. Range: 1.0 to 10.0.")
            .defineInRange("vitaGravityMultiplier", 1.5, 1.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue VITA_FALL_DAMAGE_MULTIPLIER = BUILDER
            .comment("Vita: fall-damage multiplier while Increased Gravity is active at level 1. 1.0 is",
                     "normal damage, 2.0 is double. Default: 1.5. Range: 1.0 to 10.0.")
            .defineInRange("vitaFallDamageMultiplier", 1.5, 1.0, 10.0);

    public static final ForgeConfigSpec.IntValue EL_VITA_COOLDOWN_SECONDS = BUILDER
            .comment("El Vita: cooldown in seconds after a cast. Default: 20. Range: 0 to 3600.")
            .defineInRange("elVitaCooldownSeconds", 20, 0, 3600);

    public static final ForgeConfigSpec.IntValue EL_VITA_TOTAL_MANA = BUILDER
            .comment("El Vita: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 500. Range: 1 to 100000000.")
            .defineInRange("elVitaTotalMana", 500, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EL_VITA_MANA_PER_TICK = BUILDER
            .comment("El Vita: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 5.",
                     "Range: 1 to 100000000.")
            .defineInRange("elVitaManaPerTick", 5, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_RANGE = BUILDER
            .comment("El Vita: max line-of-sight distance (blocks) to pick a target. Sneak while casting to",
                     "target yourself instead. Default: 24. Range: 1 to 64.")
            .defineInRange("elVitaRange", 24.0, 1.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_GRAVITY_MULTIPLIER = BUILDER
            .comment("El Vita: gravity multiplier applied while Increased Gravity is active at level 2 (the",
                     "tier El Vita applies — twice as heavy as Vita's level 1 by default). Default: 2.0.",
                     "Range: 1.0 to 10.0.")
            .defineInRange("elVitaGravityMultiplier", 2.0, 1.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_FALL_DAMAGE_MULTIPLIER = BUILDER
            .comment("El Vita: fall-damage multiplier while Increased Gravity is active at level 2. 1.0 is",
                     "normal damage, 2.0 is double. Default: 2.0. Range: 1.0 to 10.0.")
            .defineInRange("elVitaFallDamageMultiplier", 2.0, 1.0, 10.0);

    public static final ForgeConfigSpec.IntValue EL_VITA_CRATER_MIN_FALL_BLOCKS = BUILDER
            .comment("El Vita: minimum fall distance (blocks) before landing punches out an impact crater.",
                     "Default: 5. Range: 1 to 128.")
            .defineInRange("elVitaCraterMinFallBlocks", 5, 1, 128);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_CRATER_RADIUS = BUILDER
            .comment("El Vita: horizontal radius (blocks) of the crater punched out on a qualifying landing.",
                     "Default: 2.5. Range: 0.5 to 16.0.")
            .defineInRange("elVitaCraterRadius", 2.5, 0.5, 16.0);

    public static final ForgeConfigSpec.IntValue EL_VITA_CRATER_DEPTH = BUILDER
            .comment("El Vita: how many blocks deep the crater digs beneath the landing point. Default: 2.",
                     "Range: 1 to 16.")
            .defineInRange("elVitaCraterDepth", 2, 1, 16);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_SMASH_RADIUS = BUILDER
            .comment("El Vita: radius (blocks) of the mace-style shockwave thrown out on a qualifying landing.",
                     "Entities inside it get tossed away from the impact. Default: 3.5. Range: 0.5 to 32.0.")
            .defineInRange("elVitaSmashRadius", 3.5, 0.5, 32.0);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_SMASH_KNOCKBACK = BUILDER
            .comment("El Vita: how hard the shockwave throws entities. Doubled automatically on falls over",
                     "twice the crater threshold. Default: 0.7. Range: 0 to 5.0.")
            .defineInRange("elVitaSmashKnockback", 0.7, 0.0, 5.0);

    public static final ForgeConfigSpec.DoubleValue EL_VITA_SMASH_DAMAGE_MULTIPLIER = BUILDER
            .comment("El Vita: multiplier on the crushing damage dealt to anything you land on. The base",
                     "damage scales with fall distance exactly like a mace's smash attack. Default: 1.0.",
                     "Range: 0 to 10.0.")
            .defineInRange("elVitaSmashDamageMultiplier", 1.0, 0.0, 10.0);

    public static final ForgeConfigSpec.IntValue MURAK_TARGET_DURATION_SECONDS = BUILDER
            .comment("Murak: how long Reduced Gravity lasts when cast on someone else. Casting it on",
                     "yourself is an untimed toggle instead. Default: 60. Range: 1 to 3600.")
            .defineInRange("murakTargetDurationSeconds", 60, 1, 3600);

    public static final ForgeConfigSpec.IntValue VITA_TARGET_DURATION_SECONDS = BUILDER
            .comment("Vita: how long Increased Gravity lasts when cast on someone else. Casting it on",
                     "yourself is an untimed toggle instead. Default: 60. Range: 1 to 3600.")
            .defineInRange("vitaTargetDurationSeconds", 60, 1, 3600);

    public static final ForgeConfigSpec.IntValue EL_VITA_TARGET_DURATION_SECONDS = BUILDER
            .comment("El Vita: how long Increased Gravity lasts when cast on someone else. Casting it on",
                     "yourself is an untimed toggle instead. Default: 30. Range: 1 to 3600.")
            .defineInRange("elVitaTargetDurationSeconds", 30, 1, 3600);

    public static final ForgeConfigSpec.IntValue MURAK_SELF_UPKEEP_PER_SECOND = BUILDER
            .comment("Murak: mana drained every second while you are holding Reduced Gravity on yourself.",
                     "Casting it on someone else is a fixed-duration effect and costs nothing to sustain.",
                     "Running dry drops it automatically. Default: 25. Range: 0 to 100000000.")
            .defineInRange("murakSelfUpkeepPerSecond", 25, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue VITA_SELF_UPKEEP_PER_SECOND = BUILDER
            .comment("Vita: mana drained every second while you are holding Increased Gravity on yourself.",
                     "Casting it on someone else is a fixed-duration effect and costs nothing to sustain.",
                     "Running dry drops it automatically. Default: 25. Range: 0 to 100000000.")
            .defineInRange("vitaSelfUpkeepPerSecond", 25, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue EL_VITA_SELF_UPKEEP_PER_SECOND = BUILDER
            .comment("El Vita: mana drained every second while you are holding Increased Gravity on yourself.",
                     "Casting it on someone else is a fixed-duration effect and costs nothing to sustain.",
                     "Running dry drops it automatically. Default: 25. Range: 0 to 100000000.")
            .defineInRange("elVitaSelfUpkeepPerSecond", 25, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue TELEPORT_COOLDOWN_SECONDS = BUILDER
            .comment("Teleportation: cooldown in seconds after a cast. Default: 60. Range: 0 to 3600.")
            .defineInRange("teleportCooldownSeconds", 60, 0, 3600);

    public static final ForgeConfigSpec.IntValue TELEPORT_TOTAL_MANA = BUILDER
            .comment("Teleportation: base raw mana cost, before distance and mode are added on top.",
                     "Default: 200. Range: 1 to 100000000.")
            .defineInRange("teleportTotalMana", 200, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue TELEPORT_MANA_PER_TICK = BUILDER
            .comment("Teleportation: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 8.",
                     "Range: 1 to 100000000.")
            .defineInRange("teleportManaPerTick", 8, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue TELEPORT_MANA_PER_BLOCK = BUILDER
            .comment("Teleportation: extra raw mana per block of straight-line distance between you and the",
                     "coordinates you entered. Default: 5. Range: 0 to 100000000.")
            .defineInRange("teleportManaPerBlock", 5, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue TELEPORT_SELF_EXTRA_MANA = BUILDER
            .comment("Teleportation: extra raw mana added when you pick Self — no portal, you simply go.",
                     "Default: 100. Range: 0 to 100000000.")
            .defineInRange("teleportSelfExtraMana", 100, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue TELEPORT_PORTAL_EXTRA_MANA = BUILDER
            .comment("Teleportation: extra raw mana added when you pick Portal, which opens a two-way gate",
                     "anyone can use. Default: 200. Range: 0 to 100000000.")
            .defineInRange("teleportPortalExtraMana", 200, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue TELEPORT_PORTAL_SECONDS = BUILDER
            .comment("Teleportation: how long a portal pair stays open before closing. Default: 15.",
                     "Range: 1 to 3600.")
            .defineInRange("teleportPortalSeconds", 15, 1, 3600);

    public static final ForgeConfigSpec.DoubleValue TELEPORT_PORTAL_RADIUS = BUILDER
            .comment("Teleportation: how close you must be to a portal to be pulled through, in blocks.",
                     "Default: 1.5. Range: 0.5 to 8.")
            .defineInRange("teleportPortalRadius", 1.5, 0.5, 8.0);

    public static final ForgeConfigSpec.DoubleValue TELEPORT_PORTAL_PLACEMENT_RANGE = BUILDER
            .comment("Teleportation: how far ahead the near portal is placed, in blocks. It opens wherever you",
                     "are looking up to this distance, snapping to the free space against whatever surface you",
                     "are aiming at, or hanging in mid-air if you are aiming at nothing. Default: 7.",
                     "Range: 1 to 32.")
            .defineInRange("teleportPortalPlacementRange", 7.0, 1.0, 32.0);

    public static final ForgeConfigSpec.IntValue OL_SHAMAK_COOLDOWN_SECONDS = BUILDER
            .comment("Ol Shamak: cooldown in seconds after a cast. Default: 90. Range: 0 to 3600.")
            .defineInRange("olShamakCooldownSeconds", 90, 0, 3600);

    public static final ForgeConfigSpec.IntValue OL_SHAMAK_TOTAL_MANA = BUILDER
            .comment("Ol Shamak: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 1250. Range: 1 to 100000000.")
            .defineInRange("olShamakTotalMana", 1250, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue OL_SHAMAK_MANA_PER_TICK = BUILDER
            .comment("Ol Shamak: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 10.",
                     "Range: 1 to 100000000.")
            .defineInRange("olShamakManaPerTick", 10, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue OL_SHAMAK_RANGE = BUILDER
            .comment("Ol Shamak: max line-of-sight distance (blocks) to pick a target, and to pick a sealed",
                     "target again in order to release it. Default: 24. Range: 1 to 64.")
            .defineInRange("olShamakRange", 24.0, 1.0, 64.0);

    public static final ForgeConfigSpec.IntValue OL_SHAMAK_DURATION_SECONDS = BUILDER
            .comment("Ol Shamak: how long the seal holds before dissolving on its own. Default: 60.",
                     "Range: 1 to 3600.")
            .defineInRange("olShamakDurationSeconds", 60, 1, 3600);

    public static final ForgeConfigSpec.DoubleValue OL_SHAMAK_SEAL_HEALTH = BUILDER
            .comment("Ol Shamak: how much damage the orb absorbs before shattering and freeing whoever is",
                     "inside. Ignored when the sealed target carries a Witch Factor — then only the Dragon",
                     "Sword Reid can break it. Default: 40. Range: 1 to 10000.")
            .defineInRange("olShamakSealHealth", 40.0, 1.0, 10000.0);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_COOLDOWN_SECONDS = BUILDER
            .comment("Door Crossing: cooldown in seconds after a cast. Default: 30. Range: 0 to 3600.")
            .defineInRange("doorCrossingCooldownSeconds", 30, 0, 3600);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_TOTAL_MANA = BUILDER
            .comment("Door Crossing: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 700. Range: 1 to 100000000.")
            .defineInRange("doorCrossingTotalMana", 700, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_MANA_PER_TICK = BUILDER
            .comment("Door Crossing: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 5.",
                     "Range: 1 to 100000000.")
            .defineInRange("doorCrossingManaPerTick", 5, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue DOOR_CROSSING_RANGE = BUILDER
            .comment("Door Crossing: how far away (blocks) the door you are looking at may be. Default: 6.",
                     "Range: 1 to 32.")
            .defineInRange("doorCrossingRange", 6.0, 1.0, 32.0);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_ROOM_SIZE = BUILDER
            .comment("Door Crossing: interior width and depth (blocks) of your private room. Rooms are laid out",
                     "on a spaced grid inside the pocket dimension and never touch each other, however large",
                     "you make them. Default: 100. Range: 8 to 512.")
            .defineInRange("doorCrossingRoomSize", 100, 8, 512);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_ROOM_HEIGHT = BUILDER
            .comment("Door Crossing: interior height (blocks) of your private room. Default: 32. Range: 4 to 256.")
            .defineInRange("doorCrossingRoomHeight", 32, 4, 256);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_ENTRANCE_SECONDS = BUILDER
            .comment("Door Crossing: how long the door you cast on stays open — both as your own way in and",
                     "as a way for other players to follow you. Once it lapses that door disconnects and",
                     "nobody can enter. Default: 15. Range: 0 to 600.")
            .defineInRange("doorCrossingEntranceSeconds", 15, 0, 600);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_STRAY_DOOR_RADIUS = BUILDER
            .comment("Door Crossing: radius (blocks) around the casting point in which other doors may end up",
                     "secretly wired to your room. Only doors in already-loaded chunks are considered.",
                     "Default: 100. Range: 0 to 256.")
            .defineInRange("doorCrossingStrayDoorRadius", 100, 0, 256);

    public static final ForgeConfigSpec.IntValue DOOR_CROSSING_STRAY_DOOR_CHANCE = BUILDER
            .comment("Door Crossing: percent chance that each door inside the stray radius gets wired to your",
                     "room, letting someone wander in by mistake. These stay connected for as long as you are",
                     "inside. Default: 10. Range: 0 to 100.")
            .defineInRange("doorCrossingStrayDoorChance", 10, 0, 100);

    public static final ForgeConfigSpec.IntValue EMM_COOLDOWN_SECONDS = BUILDER
            .comment("Emm: cooldown in seconds, applied when you drop the stillness rather than when you",
                     "enter it — holding it costs you nothing extra, but coming out locks the spell down.",
                     "Default: 60. Range: 0 to 3600.")
            .defineInRange("emmCooldownSeconds", 60, 0, 3600);

    public static final ForgeConfigSpec.IntValue EMM_TOTAL_MANA = BUILDER
            .comment("Emm: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 600. Range: 1 to 100000000.")
            .defineInRange("emmTotalMana", 600, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EMM_MANA_PER_TICK = BUILDER
            .comment("Emm: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 7.",
                     "Range: 1 to 100000000.")
            .defineInRange("emmManaPerTick", 7, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EMM_UPKEEP_PER_SECOND = BUILDER
            .comment("Emm: mana drained every second while the stillness is held. Running dry drops it",
                     "automatically. Default: 50. Range: 0 to 100000000.")
            .defineInRange("emmUpkeepPerSecond", 50, 0, 100_000_000);

    public static final ForgeConfigSpec.IntValue EMT_COOLDOWN_SECONDS = BUILDER
            .comment("Emt: cooldown in seconds, applied when the field drops rather than when it goes up.",
                     "Default: 90. Range: 0 to 3600.")
            .defineInRange("emtCooldownSeconds", 90, 0, 3600);

    public static final ForgeConfigSpec.IntValue EMT_TOTAL_MANA = BUILDER
            .comment("Emt: total raw mana the spell costs to cast (before Efficiency scaling).",
                     "Default: 700. Range: 1 to 100000000.")
            .defineInRange("emtTotalMana", 700, 1, 100_000_000);

    public static final ForgeConfigSpec.IntValue EMT_MANA_PER_TICK = BUILDER
            .comment("Emt: mana-per-tick draw rate (see castFailRatio in magic_main). Default: 8.",
                     "Range: 1 to 100000000.")
            .defineInRange("emtManaPerTick", 8, 1, 100_000_000);

    public static final ForgeConfigSpec.DoubleValue EMT_RADIUS = BUILDER
            .comment("Emt: radius (blocks) of the silenced sphere. Everyone inside it, the caster included,",
                     "is cut off from casting magic and from drawing mana. Default: 25. Range: 1 to 256.")
            .defineInRange("emtRadius", 25.0, 1.0, 256.0);

    public static final ForgeConfigSpec.IntValue EMT_UPKEEP_PER_SECOND = BUILDER
            .comment("Emt: mana drained every second while the field is held. Running dry drops it",
                     "automatically. Default: 50. Range: 0 to 100000000.")
            .defineInRange("emtUpkeepPerSecond", 50, 0, 100_000_000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConfigMagicYin() {}
}
