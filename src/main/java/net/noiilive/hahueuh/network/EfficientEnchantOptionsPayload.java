package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record EfficientEnchantOptionsPayload(List<Option> options) implements CustomPacketPayload {
    public record Option(String enchantmentId, int level) {}

    public static final Type<EfficientEnchantOptionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "efficient_enchant_options"));

    public static final StreamCodec<FriendlyByteBuf, EfficientEnchantOptionsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.options().size());
                for (Option o : payload.options()) {
                    buf.writeUtf(o.enchantmentId(), 256);
                    buf.writeVarInt(o.level());
                }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Option> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(new Option(buf.readUtf(256), buf.readVarInt()));
                }
                return new EfficientEnchantOptionsPayload(list);
            });

    @Override
    public Type<EfficientEnchantOptionsPayload> type() {
        return TYPE;
    }
}
