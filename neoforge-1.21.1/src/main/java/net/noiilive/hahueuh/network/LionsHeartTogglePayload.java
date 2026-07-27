package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LionsHeartTogglePayload() implements CustomPacketPayload {
    public static final LionsHeartTogglePayload INSTANCE = new LionsHeartTogglePayload();

    public static final Type<LionsHeartTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "lions_heart_toggle"));

    public static final StreamCodec<ByteBuf, LionsHeartTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<LionsHeartTogglePayload> type() {
        return TYPE;
    }
}
