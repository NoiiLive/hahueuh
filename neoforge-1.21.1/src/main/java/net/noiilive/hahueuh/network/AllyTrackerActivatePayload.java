package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AllyTrackerActivatePayload() implements CustomPacketPayload {
    public static final AllyTrackerActivatePayload INSTANCE = new AllyTrackerActivatePayload();

    public static final Type<AllyTrackerActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ally_tracker_activate"));

    public static final StreamCodec<ByteBuf, AllyTrackerActivatePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<AllyTrackerActivatePayload> type() {
        return TYPE;
    }
}
