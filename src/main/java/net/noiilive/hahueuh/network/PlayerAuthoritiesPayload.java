package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerAuthoritiesPayload(boolean returnByDeath, boolean domain, boolean sloth, int slothVariant,
                                        boolean greed, int greedVariant, int slothHandCount,
                                        int fingerHandCount) implements CustomPacketPayload {
    public static final Type<PlayerAuthoritiesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "player_authorities"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerAuthoritiesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.returnByDeath);
                buf.writeBoolean(p.domain);
                buf.writeBoolean(p.sloth);
                buf.writeVarInt(p.slothVariant);
                buf.writeBoolean(p.greed);
                buf.writeVarInt(p.greedVariant);
                buf.writeVarInt(p.slothHandCount);
                buf.writeVarInt(p.fingerHandCount);
            },
            buf -> new PlayerAuthoritiesPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<PlayerAuthoritiesPayload> type() {
        return TYPE;
    }
}
