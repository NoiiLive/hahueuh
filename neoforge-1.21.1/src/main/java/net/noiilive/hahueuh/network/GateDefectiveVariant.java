package net.noiilive.hahueuh.network;

public enum GateDefectiveVariant {
    NO_ABSORPTION("no_absorption", "hahueuh.gate_defective.no_absorption"),
    NO_RELEASE("no_release", "hahueuh.gate_defective.no_release");

    public final String id;
    public final String translationKey;

    GateDefectiveVariant(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public static GateDefectiveVariant byId(String id) {
        if (id != null) {
            for (GateDefectiveVariant v : values()) {
                if (v.id.equalsIgnoreCase(id) || v.name().equalsIgnoreCase(id)) return v;
            }
        }
        return NO_ABSORPTION;
    }

    public static GateDefectiveVariant byOrdinal(int ordinal) {
        GateDefectiveVariant[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : NO_ABSORPTION;
    }
}
