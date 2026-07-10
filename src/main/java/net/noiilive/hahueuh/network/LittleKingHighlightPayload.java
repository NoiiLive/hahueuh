package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record LittleKingHighlightPayload(List<Integer> entityIds) implements CustomPacketPayload {
    public static final Type<LittleKingHighlightPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "little_king_highlight"));

    public static final StreamCodec<ByteBuf, LittleKingHighlightPayload> STREAM_CODEC =
            ByteBufCodecs.INT.apply(ByteBufCodecs.list())
                    .map(LittleKingHighlightPayload::new, LittleKingHighlightPayload::entityIds);

    @Override
    public Type<LittleKingHighlightPayload> type() {
        return TYPE;
    }
}
