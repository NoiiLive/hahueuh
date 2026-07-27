package net.noiilive.hahueuh.network;

public enum WitchFactorAuthority {
    NONE("none", "hahueuh.witch_factor_authority.none"),
    SLOTH("sloth", "hahueuh.authority.sloth"),
    GREED("greed", "hahueuh.authority.greed");

    public final String id;
    public final String translationKey;

    WitchFactorAuthority(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public static WitchFactorAuthority byId(String id) {
        if (id != null) {
            for (WitchFactorAuthority a : values()) {
                if (a.id.equalsIgnoreCase(id) || a.name().equalsIgnoreCase(id)) return a;
            }
        }
        return NONE;
    }
}
