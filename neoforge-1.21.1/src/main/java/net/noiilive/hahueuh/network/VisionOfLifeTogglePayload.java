package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisionOfLifeTogglePayload() implements CustomPacketPayload {
    public static final VisionOfLifeTogglePayload INSTANCE = new VisionOfLifeTogglePayload();

    public static final Type<VisionOfLifeTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_life_toggle"));

    public static final StreamCodec<ByteBuf, VisionOfLifeTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<VisionOfLifeTogglePayload> type() {
        return TYPE;
    }
}
