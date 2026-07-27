package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BackToEnchantingPayload() implements CustomPacketPayload {
    public static final BackToEnchantingPayload INSTANCE = new BackToEnchantingPayload();

    public static final Type<BackToEnchantingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "back_to_enchanting"));

    public static final StreamCodec<FriendlyByteBuf, BackToEnchantingPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<BackToEnchantingPayload> type() {
        return TYPE;
    }
}
