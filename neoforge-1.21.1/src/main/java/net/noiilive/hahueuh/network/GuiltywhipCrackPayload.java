package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GuiltywhipCrackPayload(boolean sweep) implements CustomPacketPayload {
    public static final Type<GuiltywhipCrackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "guiltywhip_crack"));

    public static final StreamCodec<ByteBuf, GuiltywhipCrackPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, GuiltywhipCrackPayload::sweep,
                    GuiltywhipCrackPayload::new);

    @Override
    public Type<GuiltywhipCrackPayload> type() {
        return TYPE;
    }
}
