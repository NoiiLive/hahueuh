package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record MurakStatePayload(boolean reducedGravity, boolean flying) implements CustomPacketPayload {
    public static final Type<MurakStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "murak_state"));

    public static final StreamCodec<ByteBuf, MurakStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MurakStatePayload::reducedGravity,
            ByteBufCodecs.BOOL, MurakStatePayload::flying,
            MurakStatePayload::new);

    @Override
    public Type<MurakStatePayload> type() {
        return TYPE;
    }
}
