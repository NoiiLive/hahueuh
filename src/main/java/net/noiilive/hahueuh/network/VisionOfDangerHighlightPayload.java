package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record VisionOfDangerHighlightPayload(List<Integer> entityIds) implements CustomPacketPayload {
    public static final Type<VisionOfDangerHighlightPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_danger_highlight"));

    public static final StreamCodec<ByteBuf, VisionOfDangerHighlightPayload> STREAM_CODEC =
            ByteBufCodecs.INT.apply(ByteBufCodecs.list())
                    .map(VisionOfDangerHighlightPayload::new, VisionOfDangerHighlightPayload::entityIds);

    @Override
    public Type<VisionOfDangerHighlightPayload> type() {
        return TYPE;
    }
}
