package net.noiilive.hahueuh;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChunkMana(double amount, long lastTick) {
    public static final ChunkMana UNINITIALIZED = new ChunkMana(0.0, Long.MIN_VALUE);

    public static final Codec<ChunkMana> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(ChunkMana::amount),
            Codec.LONG.fieldOf("lastTick").forGetter(ChunkMana::lastTick)
    ).apply(inst, ChunkMana::new));

    public boolean uninitialized() {
        return lastTick == Long.MIN_VALUE;
    }
}
