package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record ElVitaActivatePayload() implements CustomPacketPayload {
    public static final ElVitaActivatePayload INSTANCE = new ElVitaActivatePayload();

    public static final Type<ElVitaActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "el_vita_activate"));

    public static final StreamCodec<ByteBuf, ElVitaActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<ElVitaActivatePayload> type() {
        return TYPE;
    }
}
