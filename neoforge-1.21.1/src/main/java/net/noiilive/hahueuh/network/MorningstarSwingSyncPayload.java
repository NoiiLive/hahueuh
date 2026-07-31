package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MorningstarSwingSyncPayload(UUID owner, boolean spin) implements CustomPacketPayload {
    public static final Type<MorningstarSwingSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "morningstar_swing_sync"));

    public static final StreamCodec<ByteBuf, MorningstarSwingSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, MorningstarSwingSyncPayload::owner,
                    ByteBufCodecs.BOOL, MorningstarSwingSyncPayload::spin,
                    MorningstarSwingSyncPayload::new);

    @Override
    public Type<MorningstarSwingSyncPayload> type() {
        return TYPE;
    }
}
