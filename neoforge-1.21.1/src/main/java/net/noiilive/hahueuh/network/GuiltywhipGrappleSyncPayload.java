package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GuiltywhipGrappleSyncPayload(UUID owner, int targetId, boolean hasBlock,
                                           double x, double y, double z)
        implements CustomPacketPayload {
    public static final Type<GuiltywhipGrappleSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "guiltywhip_grapple_sync"));

    public static final StreamCodec<ByteBuf, GuiltywhipGrappleSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        UUIDUtil.STREAM_CODEC.encode(buf, payload.owner());
                        ByteBufCodecs.VAR_INT.encode(buf, payload.targetId());
                        buf.writeBoolean(payload.hasBlock());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                    },
                    buf -> new GuiltywhipGrappleSyncPayload(
                            UUIDUtil.STREAM_CODEC.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            buf.readBoolean(),
                            buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Type<GuiltywhipGrappleSyncPayload> type() {
        return TYPE;
    }
}
