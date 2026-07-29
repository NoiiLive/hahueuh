package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record EmtActivatePayload() implements CustomPacketPayload {
    public static final EmtActivatePayload INSTANCE = new EmtActivatePayload();

    public static final Type<EmtActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "emt_activate"));

    public static final StreamCodec<ByteBuf, EmtActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<EmtActivatePayload> type() {
        return TYPE;
    }
}
