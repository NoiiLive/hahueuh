package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.ConfigGreed;
import net.minecraft.util.RandomSource;

public enum GreedVariant {
    LIONSHEART("lionsheart", "Lion's Heart", "hahueuh.variant.lionsheart"),
    CORLEONIS("corleonis", "Cor Leonis", "hahueuh.variant.corleonis"),
    ECHIDNA("echidna", "Echidna", "hahueuh.variant.echidna");

    public final String id;
    public final String displayName;
    public final String translationKey;

    GreedVariant(String id, String displayName, String translationKey) {
        this.id = id;
        this.displayName = displayName;
        this.translationKey = translationKey;
    }

    public static GreedVariant byId(String id) {
        if (id != null) {
            for (GreedVariant v : values()) {
                if (v.id.equalsIgnoreCase(id) || v.name().equalsIgnoreCase(id)) return v;
            }
        }
        return LIONSHEART;
    }

    public static GreedVariant byOrdinal(int ordinal) {
        GreedVariant[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : LIONSHEART;
    }

    public static GreedVariant randomWeighted(RandomSource random) {
        int wLion = Math.max(0, ConfigGreed.VARIANT_WEIGHT_LIONSHEART.getAsInt());
        int wCor = Math.max(0, ConfigGreed.VARIANT_WEIGHT_CORLEONIS.getAsInt());
        int wEchidna = Math.max(0, ConfigGreed.VARIANT_WEIGHT_ECHIDNA.getAsInt());
        int total = wLion + wCor + wEchidna;
        if (total <= 0) return LIONSHEART;

        int roll = random.nextInt(total);
        if (roll < wLion) return LIONSHEART;
        if (roll < wLion + wCor) return CORLEONIS;
        return ECHIDNA;
    }

    private static final GreedVariant[] MOB_ELIGIBLE = {LIONSHEART};

    public static GreedVariant randomForMob(RandomSource random) {
        return MOB_ELIGIBLE[random.nextInt(MOB_ELIGIBLE.length)];
    }
}
