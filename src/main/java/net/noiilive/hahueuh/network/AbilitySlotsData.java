package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AbilitySlotsData(List<String> slots, int cycleGroup, boolean hudHidden) {
    public static final int SLOT_COUNT = 9;

    public static AbilitySlotsData empty() {
        return new AbilitySlotsData(new ArrayList<>(Collections.nCopies(SLOT_COUNT, "")), 0, false);
    }

    public static final StreamCodec<ByteBuf, AbilitySlotsData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AbilitySlotsData::slots,
            ByteBufCodecs.VAR_INT, AbilitySlotsData::cycleGroup,
            ByteBufCodecs.BOOL, AbilitySlotsData::hudHidden,
            AbilitySlotsData::new);
}
