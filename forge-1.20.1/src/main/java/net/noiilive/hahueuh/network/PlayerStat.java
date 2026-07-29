package net.noiilive.hahueuh.network;

public enum PlayerStat {
    TENACITY("tenacity"),
    FORTITUDE("fortitude"),
    STRENGTH("strength"),
    REFLEXES("reflexes"),
    MAGIC("magic"),
    COMBAT("combat");

    public final String id;
    public final String translationKey;

    PlayerStat(String id) {
        this.id = id;
        this.translationKey = "hahueuh.stat." + id;
    }

    public static final PlayerStat[] ORDERED = values();

    public static PlayerStat byId(String id) {
        for (PlayerStat stat : ORDERED) {
            if (stat.id.equals(id)) return stat;
        }
        return TENACITY;
    }
}
