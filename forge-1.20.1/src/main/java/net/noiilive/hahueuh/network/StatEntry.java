package net.noiilive.hahueuh.network;

import net.minecraft.nbt.CompoundTag;

public record StatEntry(int proficiency, int capacity, int level, int progress) {
    public static final StatEntry UNROLLED = new StatEntry(0, 0, 0, 0);

    public boolean rolled() {
        return capacity > 0;
    }

    public StatEntry withProgress(int newProgress) {
        return new StatEntry(proficiency, capacity, level, newProgress);
    }

    public StatEntry withLevelAndProgress(int newLevel, int newProgress) {
        return new StatEntry(proficiency, capacity, newLevel, newProgress);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Proficiency", proficiency);
        tag.putInt("Capacity", capacity);
        tag.putInt("Level", level);
        tag.putInt("Progress", progress);
        return tag;
    }

    public static StatEntry load(CompoundTag tag) {
        return new StatEntry(tag.getInt("Proficiency"), tag.getInt("Capacity"),
                tag.getInt("Level"), tag.getInt("Progress"));
    }
}
