package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record FingerHighlightPayload(List<Integer> entityIds) implements CustomPacketPayload {
    public static final Type<FingerHighlightPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger_highlight"));

    public static final StreamCodec<ByteBuf, FingerHighlightPayload> STREAM_CODEC =
            ByteBufCodecs.INT.apply(ByteBufCodecs.list())
                    .map(FingerHighlightPayload::new, FingerHighlightPayload::entityIds);

    @Override
    public Type<FingerHighlightPayload> type() {
        return TYPE;
    }
}
