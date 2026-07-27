package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StoreSpellPayload(ResourceLocation spellId) implements CustomPacketPayload {
    public static final Type<StoreSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "store_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoreSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, StoreSpellPayload::spellId,
            StoreSpellPayload::new);

    @Override
    public Type<StoreSpellPayload> type() {
        return TYPE;
    }
}
