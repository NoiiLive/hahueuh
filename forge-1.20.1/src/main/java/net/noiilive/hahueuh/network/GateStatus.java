package net.noiilive.hahueuh.network;

public enum GateStatus {
    OPEN("open", "hahueuh.gate_status.open"),
    PARTLY_OPEN("partly_open", "hahueuh.gate_status.partly_open"),
    DAMAGED("damaged", "hahueuh.gate_status.damaged"),
    DESTROYED("destroyed", "hahueuh.gate_status.destroyed"),
    DEFECTIVE("defective", "hahueuh.gate_status.defective");

    public final String id;
    public final String translationKey;

    GateStatus(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public static GateStatus byId(String id) {
        if (id != null) {
            for (GateStatus s : values()) {
                if (s.id.equalsIgnoreCase(id) || s.name().equalsIgnoreCase(id)) return s;
            }
        }
        return OPEN;
    }

    public static GateStatus byOrdinal(int ordinal) {
        GateStatus[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : OPEN;
    }
}
