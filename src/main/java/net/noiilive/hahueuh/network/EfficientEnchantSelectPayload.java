package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EfficientEnchantSelectPayload(String enchantmentId) implements CustomPacketPayload {
    public static final Type<EfficientEnchantSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "efficient_enchant_select"));

    public static final StreamCodec<FriendlyByteBuf, EfficientEnchantSelectPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.enchantmentId(), 256),
            buf -> new EfficientEnchantSelectPayload(buf.readUtf(256)));

    @Override
    public Type<EfficientEnchantSelectPayload> type() {
        return TYPE;
    }
}
