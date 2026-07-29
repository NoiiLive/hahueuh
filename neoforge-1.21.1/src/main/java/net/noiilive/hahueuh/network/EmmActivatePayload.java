package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record EmmActivatePayload() implements CustomPacketPayload {
    public static final EmmActivatePayload INSTANCE = new EmmActivatePayload();

    public static final Type<EmmActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "emm_activate"));

    public static final StreamCodec<ByteBuf, EmmActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<EmmActivatePayload> type() {
        return TYPE;
    }
}
