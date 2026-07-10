package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateAuthorityPayload(boolean aggressor) implements CustomPacketPayload {
    public static final Type<ActivateAuthorityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "activate_authority"));

    public static final StreamCodec<ByteBuf, ActivateAuthorityPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ActivateAuthorityPayload::new, ActivateAuthorityPayload::aggressor);

    @Override
    public Type<ActivateAuthorityPayload> type() {
        return TYPE;
    }
}
