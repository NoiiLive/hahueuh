package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MorningstarHeadPayload(UUID owner, double x, double y, double z,
                                     double vx, double vy, double vz)
        implements CustomPacketPayload {
    public static final Type<MorningstarHeadPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "morningstar_head"));

    public static final StreamCodec<ByteBuf, MorningstarHeadPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        UUIDUtil.STREAM_CODEC.encode(buf, payload.owner());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeDouble(payload.vx());
                        buf.writeDouble(payload.vy());
                        buf.writeDouble(payload.vz());
                    },
                    buf -> new MorningstarHeadPayload(
                            UUIDUtil.STREAM_CODEC.decode(buf),
                            buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Type<MorningstarHeadPayload> type() {
        return TYPE;
    }
}
