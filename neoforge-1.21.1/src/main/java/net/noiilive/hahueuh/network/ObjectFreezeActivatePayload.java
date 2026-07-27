package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ObjectFreezeActivatePayload() implements CustomPacketPayload {
    public static final ObjectFreezeActivatePayload INSTANCE = new ObjectFreezeActivatePayload();

    public static final Type<ObjectFreezeActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "object_freeze_activate"));

    public static final StreamCodec<ByteBuf, ObjectFreezeActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<ObjectFreezeActivatePayload> type() {
        return TYPE;
    }
}
