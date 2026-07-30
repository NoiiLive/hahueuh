package net.noiilive.hahueuh;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigDomain {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue DOMAIN_RADIUS = BUILDER
            .comment("Full width (edge to edge, in blocks) of a Domain, centered on the Matrix (the",
                     "point where it was opened). So 100 means the dome spans 100 blocks across and",
                     "reaches 50 blocks in every direction; wandering past that closes the domain.",
                     "Default: 100. Range: 5 to 500.")
            .defineInRange("domainRadius", 100, 5, 500);

    public static final ForgeConfigSpec.IntValue DOMAIN_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after a Domain closes before that player can open another.",
                     "Applies however the domain closed (manual, leaving the radius, death, etc.).",
                     "Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("domainCooldownSeconds", 10, 0, 3600);

    public static final ForgeConfigSpec.DoubleValue DOMAIN_AGGRESSOR_MAX_HEALTH = BUILDER
            .comment("Aggressor Domain: the most health (in half-hearts) you may have and still open one.",
                     "Al only reaches for it on the brink of death, so by default you must be at 2 hearts",
                     "or less. Creative players ignore this. Victim Domain is unaffected.",
                     "Default: 4. Range: 1 to 1024.")
            .defineInRange("domainAggressorMaxHealth", 4.0, 1.0, 1024.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConfigDomain() {}
}
