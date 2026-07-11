package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AllyTrackerRefreshPayload() implements CustomPacketPayload {
    public static final AllyTrackerRefreshPayload INSTANCE = new AllyTrackerRefreshPayload();

    public static final Type<AllyTrackerRefreshPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ally_tracker_refresh"));

    public static final StreamCodec<ByteBuf, AllyTrackerRefreshPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<AllyTrackerRefreshPayload> type() {
        return TYPE;
    }
}
