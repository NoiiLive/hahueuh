package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record DoorCrossingActivatePayload() implements CustomPacketPayload {
    public static final DoorCrossingActivatePayload INSTANCE = new DoorCrossingActivatePayload();

    public static final Type<DoorCrossingActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "door_crossing_activate"));

    public static final StreamCodec<ByteBuf, DoorCrossingActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<DoorCrossingActivatePayload> type() {
        return TYPE;
    }
}
