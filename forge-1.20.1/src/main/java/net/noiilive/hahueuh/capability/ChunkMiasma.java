package net.noiilive.hahueuh.capability;

import net.minecraft.nbt.CompoundTag;

public class ChunkMiasma {
    private double amount = -1.0;
    private long lastTick;

    public boolean uninitialized() {
        return amount < 0.0;
    }

    public double amount() {
        return amount;
    }

    public long lastTick() {
        return lastTick;
    }

    public void set(double amount, long lastTick) {
        this.amount = amount;
        this.lastTick = lastTick;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Amount", amount);
        tag.putLong("LastTick", lastTick);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        amount = tag.contains("Amount") ? tag.getDouble("Amount") : -1.0;
        lastTick = tag.getLong("LastTick");
    }
}
