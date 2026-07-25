package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManaChargePayload(boolean charging) implements CustomPacketPayload {
    public static final Type<ManaChargePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "mana_charge"));

    public static final StreamCodec<ByteBuf, ManaChargePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ManaChargePayload::new, ManaChargePayload::charging);

    @Override
    public Type<ManaChargePayload> type() {
        return TYPE;
    }
}
