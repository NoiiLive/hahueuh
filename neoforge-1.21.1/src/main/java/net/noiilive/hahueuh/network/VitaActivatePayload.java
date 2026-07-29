package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record VitaActivatePayload() implements CustomPacketPayload {
    public static final VitaActivatePayload INSTANCE = new VitaActivatePayload();

    public static final Type<VitaActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vita_activate"));

    public static final StreamCodec<ByteBuf, VitaActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<VitaActivatePayload> type() {
        return TYPE;
    }
}
