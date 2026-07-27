package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CastSpellPayload(ResourceLocation spellId) implements CustomPacketPayload {
    public static final Type<CastSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "cast_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ResourceLocation.STREAM_CODEC.encode(buf, p.spellId),
            buf -> new CastSpellPayload(ResourceLocation.STREAM_CODEC.decode(buf)));

    @Override
    public Type<CastSpellPayload> type() {
        return TYPE;
    }
}
