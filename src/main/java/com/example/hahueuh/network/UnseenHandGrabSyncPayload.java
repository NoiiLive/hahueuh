package com.example.hahueuh.network;

import com.example.hahueuh.HahUeuh;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record UnseenHandGrabSyncPayload(UUID owner, List<Integer> grabbedIds) implements CustomPacketPayload {
    public static final Type<UnseenHandGrabSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand_grab_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnseenHandGrabSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, UnseenHandGrabSyncPayload::owner,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), UnseenHandGrabSyncPayload::grabbedIds,
            UnseenHandGrabSyncPayload::new);

    @Override
    public Type<UnseenHandGrabSyncPayload> type() {
        return TYPE;
    }
}
