package com.example.hahueuh.network;

import com.example.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateAuthorityPayload() implements CustomPacketPayload {
    public static final Type<ActivateAuthorityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "activate_authority"));

    public static final ActivateAuthorityPayload INSTANCE = new ActivateAuthorityPayload();

    public static final StreamCodec<ByteBuf, ActivateAuthorityPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<ActivateAuthorityPayload> type() {
        return TYPE;
    }
}
