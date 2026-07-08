package com.example.hahueuh.network;

import com.example.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeathFadePayload(boolean toBlack) implements CustomPacketPayload {
    public static final Type<DeathFadePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "death_fade"));

    public static final StreamCodec<ByteBuf, DeathFadePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(DeathFadePayload::new, DeathFadePayload::toBlack);

    @Override
    public Type<DeathFadePayload> type() {
        return TYPE;
    }
}
