package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisionInfoQueryPayload(String query) implements CustomPacketPayload {
    public static final Type<VisionInfoQueryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_info_query"));

    public static final StreamCodec<FriendlyByteBuf, VisionInfoQueryPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.query(), 64),
            buf -> new VisionInfoQueryPayload(buf.readUtf(64)));

    @Override
    public Type<VisionInfoQueryPayload> type() {
        return TYPE;
    }
}
