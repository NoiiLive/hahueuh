package net.noiilive.hahueuh.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record StatEntry(int proficiency, int capacity, int level, int progress) {
    public static final StatEntry UNROLLED = new StatEntry(0, 0, 0, 0);

    public static final Codec<StatEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("proficiency").forGetter(StatEntry::proficiency),
            Codec.INT.fieldOf("capacity").forGetter(StatEntry::capacity),
            Codec.INT.fieldOf("level").forGetter(StatEntry::level),
            Codec.INT.fieldOf("progress").forGetter(StatEntry::progress)
    ).apply(inst, StatEntry::new));

    public static final StreamCodec<FriendlyByteBuf, StatEntry> STREAM_CODEC = StreamCodec.of(
            (buf, e) -> {
                buf.writeVarInt(e.proficiency);
                buf.writeVarInt(e.capacity);
                buf.writeVarInt(e.level);
                buf.writeVarInt(e.progress);
            },
            buf -> new StatEntry(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    public boolean rolled() {
        return capacity > 0;
    }

    public StatEntry withProgress(int newProgress) {
        return new StatEntry(proficiency, capacity, level, newProgress);
    }

    public StatEntry withLevelAndProgress(int newLevel, int newProgress) {
        return new StatEntry(proficiency, capacity, newLevel, newProgress);
    }
}
