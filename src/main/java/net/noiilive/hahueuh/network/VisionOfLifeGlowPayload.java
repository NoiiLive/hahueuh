package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record VisionOfLifeGlowPayload(List<Integer> hostileIds, List<Integer> passiveIds, List<Integer> playerIds,
                                       List<Integer> witchFactorIds)
        implements CustomPacketPayload {
    public static final Type<VisionOfLifeGlowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_life_glow"));

    public static final StreamCodec<ByteBuf, VisionOfLifeGlowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), VisionOfLifeGlowPayload::hostileIds,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), VisionOfLifeGlowPayload::passiveIds,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), VisionOfLifeGlowPayload::playerIds,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), VisionOfLifeGlowPayload::witchFactorIds,
            VisionOfLifeGlowPayload::new);

    @Override
    public Type<VisionOfLifeGlowPayload> type() {
        return TYPE;
    }
}
