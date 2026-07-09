package com.example.hahueuh.network;

public enum GreedVariant {
    LIONSHEART("lionsheart", "Lion's Heart", "hahueuh.variant.lionsheart"),
    CORLEONIS("corleonis", "Cor Leonis", "hahueuh.variant.corleonis");

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
}
