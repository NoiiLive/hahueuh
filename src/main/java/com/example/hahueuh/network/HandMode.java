package com.example.hahueuh.network;

public enum HandMode {
    NONE,
    ATTACK,
    GRAB;

    private static final HandMode[] VALUES = values();

    public static HandMode byId(int id) {
        return (id >= 0 && id < VALUES.length) ? VALUES[id] : NONE;
    }
}
