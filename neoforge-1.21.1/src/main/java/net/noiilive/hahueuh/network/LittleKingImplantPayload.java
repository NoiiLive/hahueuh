package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LittleKingImplantPayload() implements CustomPacketPayload {
    public static final LittleKingImplantPayload INSTANCE = new LittleKingImplantPayload();

    public static final Type<LittleKingImplantPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "little_king_implant"));

    public static final StreamCodec<ByteBuf, LittleKingImplantPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<LittleKingImplantPayload> type() {
        return TYPE;
    }
}
