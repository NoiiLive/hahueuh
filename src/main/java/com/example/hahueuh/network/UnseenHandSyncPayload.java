package com.example.hahueuh.network;

import com.example.hahueuh.HahUeuh;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record UnseenHandSyncPayload(UUID owner, boolean active, float distance, int mode, int variant, boolean mobility) implements CustomPacketPayload {
    public static final Type<UnseenHandSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnseenHandSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, UnseenHandSyncPayload::owner,
            ByteBufCodecs.BOOL, UnseenHandSyncPayload::active,
            ByteBufCodecs.FLOAT, UnseenHandSyncPayload::distance,
            ByteBufCodecs.VAR_INT, UnseenHandSyncPayload::mode,
            ByteBufCodecs.VAR_INT, UnseenHandSyncPayload::variant,
            ByteBufCodecs.BOOL, UnseenHandSyncPayload::mobility,
            UnseenHandSyncPayload::new);

    @Override
    public Type<UnseenHandSyncPayload> type() {
        return TYPE;
    }
}
