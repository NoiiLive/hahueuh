package com.example.hahueuh.network;

import com.example.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LionsHeartStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<LionsHeartStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "lions_heart_state"));

    public static final StreamCodec<ByteBuf, LionsHeartStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(LionsHeartStatePayload::new, LionsHeartStatePayload::active);

    @Override
    public Type<LionsHeartStatePayload> type() {
        return TYPE;
    }
}
