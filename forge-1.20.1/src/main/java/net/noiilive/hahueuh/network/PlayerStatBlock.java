package net.noiilive.hahueuh.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PlayerStatBlock(List<StatEntry> entries) {
    public static final PlayerStatBlock UNROLLED =
            new PlayerStatBlock(Collections.nCopies(PlayerStat.ORDERED.length, StatEntry.UNROLLED));

    private static PlayerStatBlock of(List<StatEntry> raw) {
        List<StatEntry> padded = new ArrayList<>(PlayerStat.ORDERED.length);
        for (int i = 0; i < PlayerStat.ORDERED.length; i++) {
            padded.add(i < raw.size() ? raw.get(i) : StatEntry.UNROLLED);
        }
        return new PlayerStatBlock(List.copyOf(padded));
    }

    public StatEntry get(PlayerStat stat) {
        int i = stat.ordinal();
        return i < entries.size() ? entries.get(i) : StatEntry.UNROLLED;
    }

    public PlayerStatBlock with(PlayerStat stat, StatEntry entry) {
        List<StatEntry> copy = new ArrayList<>(entries);
        while (copy.size() < PlayerStat.ORDERED.length) copy.add(StatEntry.UNROLLED);
        copy.set(stat.ordinal(), entry);
        return new PlayerStatBlock(List.copyOf(copy));
    }

    public boolean rolled() {
        return get(PlayerStat.TENACITY).rolled();
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (StatEntry entry : entries) list.add(entry.save());
        return list;
    }

    public static PlayerStatBlock load(ListTag list) {
        if (list == null || list.isEmpty()) return UNROLLED;
        List<StatEntry> raw = new ArrayList<>(list.size());
        for (Tag tag : list) {
            raw.add(tag instanceof CompoundTag compound ? StatEntry.load(compound) : StatEntry.UNROLLED);
        }
        return of(raw);
    }
}
