package net.noiilive.hahueuh.network;

public enum PlayerRace {
    HUMAN("human", "hahueuh.race.human"),
    ELF("elf", "hahueuh.race.elf"),
    HALF_ELF("half_elf", "hahueuh.race.half_elf");

    public final String id;
    public final String translationKey;

    PlayerRace(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public static PlayerRace byId(String id) {
        if (id != null) {
            for (PlayerRace r : values()) {
                if (r.id.equalsIgnoreCase(id) || r.name().equalsIgnoreCase(id)) return r;
            }
        }
        return HUMAN;
    }

    public static PlayerRace byOrdinal(int ordinal) {
        PlayerRace[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : HUMAN;
    }
}
