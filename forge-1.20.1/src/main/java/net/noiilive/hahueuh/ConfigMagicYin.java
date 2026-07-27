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

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConfigMagicYin() {}
}
