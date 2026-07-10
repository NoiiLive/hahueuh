package net.noiilive.hahueuh;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigDomain {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DOMAIN_RADIUS = BUILDER
            .comment("Full width (edge to edge, in blocks) of a Domain, centered on the Matrix (the",
                     "point where it was opened). So 100 means the dome spans 100 blocks across and",
                     "reaches 50 blocks in every direction; wandering past that closes the domain.",
                     "Default: 100. Range: 5 to 500.")
            .defineInRange("domainRadius", 100, 5, 500);

    public static final ModConfigSpec.IntValue DOMAIN_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (in seconds) after a Domain closes before that player can open another.",
                     "Applies however the domain closed (manual, leaving the radius, death, etc.).",
                     "Set to 0 to disable. Default: 10. Range: 0 to 3600.")
            .defineInRange("domainCooldownSeconds", 10, 0, 3600);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigDomain() {}
}
