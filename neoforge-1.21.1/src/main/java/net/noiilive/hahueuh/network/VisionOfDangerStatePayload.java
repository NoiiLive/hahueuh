package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisionOfDangerStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<VisionOfDangerStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_danger_state"));

    public static final StreamCodec<ByteBuf, VisionOfDangerStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(VisionOfDangerStatePayload::new, VisionOfDangerStatePayload::active);

    @Override
    public Type<VisionOfDangerStatePayload> type() {
        return TYPE;
    }
}
