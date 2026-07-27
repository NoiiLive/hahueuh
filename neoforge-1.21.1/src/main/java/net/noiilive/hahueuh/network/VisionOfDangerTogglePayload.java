package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisionOfDangerTogglePayload() implements CustomPacketPayload {
    public static final VisionOfDangerTogglePayload INSTANCE = new VisionOfDangerTogglePayload();

    public static final Type<VisionOfDangerTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_danger_toggle"));

    public static final StreamCodec<ByteBuf, VisionOfDangerTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<VisionOfDangerTogglePayload> type() {
        return TYPE;
    }
}
