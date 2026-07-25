package net.noiilive.hahueuh;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChunkMiasma(double amount, long lastTick) {
    public static final ChunkMiasma UNINITIALIZED = new ChunkMiasma(0.0, Long.MIN_VALUE);

    public static final Codec<ChunkMiasma> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(ChunkMiasma::amount),
            Codec.LONG.fieldOf("lastTick").forGetter(ChunkMiasma::lastTick)
    ).apply(inst, ChunkMiasma::new));

    public boolean uninitialized() {
        return lastTick == Long.MIN_VALUE;
    }
}
