package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AlShamakActivatePayload(int mode, int targetEntityId) implements CustomPacketPayload {
    public static final int RELEASE = 0;
    public static final int BANISH = 1;
    public static final int DISCARD = 2;

    public static final Type<AlShamakActivatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "al_shamak_activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlShamakActivatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AlShamakActivatePayload::mode,
            ByteBufCodecs.VAR_INT, AlShamakActivatePayload::targetEntityId,
            AlShamakActivatePayload::new);

    @Override
    public Type<AlShamakActivatePayload> type() {
        return TYPE;
    }
}
