package net.noiilive.hahueuh.network;

public enum AllyType {
    PLAYER(0xFFFFFFFF),
    TAMED(0xFF55FF55),
    PASSIVE(0xFFFFFF55);

    public final int dotColor;

    AllyType(int dotColor) {
        this.dotColor = dotColor;
    }

    public static AllyType byOrdinal(int ordinal) {
        AllyType[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : PASSIVE;
    }
}
