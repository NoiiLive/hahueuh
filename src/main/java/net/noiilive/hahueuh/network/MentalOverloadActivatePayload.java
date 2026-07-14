package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MentalOverloadActivatePayload() implements CustomPacketPayload {
    public static final MentalOverloadActivatePayload INSTANCE = new MentalOverloadActivatePayload();

    public static final Type<MentalOverloadActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "mental_overload_activate"));

    public static final StreamCodec<ByteBuf, MentalOverloadActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<MentalOverloadActivatePayload> type() {
        return TYPE;
    }
}
