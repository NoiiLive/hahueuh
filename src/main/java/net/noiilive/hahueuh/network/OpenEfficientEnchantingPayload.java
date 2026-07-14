package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenEfficientEnchantingPayload() implements CustomPacketPayload {
    public static final OpenEfficientEnchantingPayload INSTANCE = new OpenEfficientEnchantingPayload();

    public static final Type<OpenEfficientEnchantingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "open_efficient_enchanting"));

    public static final StreamCodec<FriendlyByteBuf, OpenEfficientEnchantingPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenEfficientEnchantingPayload> type() {
        return TYPE;
    }
}
