package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnseenHandPayload(boolean active, float distance, int mode, boolean mobility) implements CustomPacketPayload {
    public static final Type<UnseenHandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand"));

    public static final StreamCodec<ByteBuf, UnseenHandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, UnseenHandPayload::active,
            ByteBufCodecs.FLOAT, UnseenHandPayload::distance,
            ByteBufCodecs.VAR_INT, UnseenHandPayload::mode,
            ByteBufCodecs.BOOL, UnseenHandPayload::mobility,
            UnseenHandPayload::new);

    @Override
    public Type<UnseenHandPayload> type() {
        return TYPE;
    }
}
