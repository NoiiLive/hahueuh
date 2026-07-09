package com.example.hahueuh.network;

import com.example.hahueuh.HahUeuh;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerAuthoritiesPayload(boolean returnByDeath, boolean domain, boolean sloth, int slothVariant,
                                        boolean greed, int greedVariant) implements CustomPacketPayload {
    public static final Type<PlayerAuthoritiesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "player_authorities"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PlayerAuthoritiesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, PlayerAuthoritiesPayload::returnByDeath,
                    ByteBufCodecs.BOOL, PlayerAuthoritiesPayload::domain,
                    ByteBufCodecs.BOOL, PlayerAuthoritiesPayload::sloth,
                    ByteBufCodecs.VAR_INT, PlayerAuthoritiesPayload::slothVariant,
                    ByteBufCodecs.BOOL, PlayerAuthoritiesPayload::greed,
                    ByteBufCodecs.VAR_INT, PlayerAuthoritiesPayload::greedVariant,
                    PlayerAuthoritiesPayload::new);

    @Override
    public Type<PlayerAuthoritiesPayload> type() {
        return TYPE;
    }
}
