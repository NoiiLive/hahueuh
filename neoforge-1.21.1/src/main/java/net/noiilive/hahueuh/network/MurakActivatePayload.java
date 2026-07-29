package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record MurakActivatePayload() implements CustomPacketPayload {
    public static final MurakActivatePayload INSTANCE = new MurakActivatePayload();

    public static final Type<MurakActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "murak_activate"));

    public static final StreamCodec<ByteBuf, MurakActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<MurakActivatePayload> type() {
        return TYPE;
    }
}
