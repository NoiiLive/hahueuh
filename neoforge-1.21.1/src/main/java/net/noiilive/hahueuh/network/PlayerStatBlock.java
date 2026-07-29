package net.noiilive.hahueuh.network;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PlayerStatBlock(List<StatEntry> entries) {
    public static final PlayerStatBlock UNROLLED =
            new PlayerStatBlock(Collections.nCopies(PlayerStat.ORDERED.length, StatEntry.UNROLLED));

    public static final Codec<PlayerStatBlock> CODEC =
            StatEntry.CODEC.listOf().xmap(PlayerStatBlock::of, PlayerStatBlock::entries);

    public static final StreamCodec<FriendlyByteBuf, PlayerStatBlock> STREAM_CODEC = StreamCodec.of(
            (buf, block) -> {
                buf.writeVarInt(block.entries.size());
                for (StatEntry entry : block.entries) StatEntry.STREAM_CODEC.encode(buf, entry);
            },
            buf -> {
                int size = buf.readVarInt();
                List<StatEntry> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(StatEntry.STREAM_CODEC.decode(buf));
                return of(list);
            });

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
}
