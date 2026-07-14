package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisionOfLifeStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<VisionOfLifeStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_life_state"));

    public static final StreamCodec<ByteBuf, VisionOfLifeStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(VisionOfLifeStatePayload::new, VisionOfLifeStatePayload::active);

    @Override
    public Type<VisionOfLifeStatePayload> type() {
        return TYPE;
    }
}
