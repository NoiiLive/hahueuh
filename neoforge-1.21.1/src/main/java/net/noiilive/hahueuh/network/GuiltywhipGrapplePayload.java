package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GuiltywhipGrapplePayload(int targetId, boolean hasBlock, double x, double y, double z)
        implements CustomPacketPayload {
    public static final Type<GuiltywhipGrapplePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "guiltywhip_grapple"));

    public static final StreamCodec<ByteBuf, GuiltywhipGrapplePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GuiltywhipGrapplePayload::targetId,
                    ByteBufCodecs.BOOL, GuiltywhipGrapplePayload::hasBlock,
                    ByteBufCodecs.DOUBLE, GuiltywhipGrapplePayload::x,
                    ByteBufCodecs.DOUBLE, GuiltywhipGrapplePayload::y,
                    ByteBufCodecs.DOUBLE, GuiltywhipGrapplePayload::z,
                    GuiltywhipGrapplePayload::new);

    @Override
    public Type<GuiltywhipGrapplePayload> type() {
        return TYPE;
    }
}
