package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UlMinyaActivatePayload(int targetEntityId) implements CustomPacketPayload {
    public static final Type<UlMinyaActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ul_minya_activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UlMinyaActivatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UlMinyaActivatePayload::targetEntityId,
            UlMinyaActivatePayload::new);

    @Override
    public Type<UlMinyaActivatePayload> type() {
        return TYPE;
    }
}
