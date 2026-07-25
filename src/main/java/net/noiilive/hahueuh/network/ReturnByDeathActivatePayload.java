package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReturnByDeathActivatePayload() implements CustomPacketPayload {
    public static final ReturnByDeathActivatePayload INSTANCE = new ReturnByDeathActivatePayload();

    public static final Type<ReturnByDeathActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "return_by_death_activate"));

    public static final StreamCodec<ByteBuf, ReturnByDeathActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<ReturnByDeathActivatePayload> type() {
        return TYPE;
    }
}
