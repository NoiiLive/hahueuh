package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record UnseenHandSyncPayload(UUID owner, int entityId, boolean active, float distance, int mode, int variant, boolean mobility) implements CustomPacketPayload {
    public static final Type<UnseenHandSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnseenHandSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.owner);
                buf.writeVarInt(p.entityId);
                buf.writeBoolean(p.active);
                buf.writeFloat(p.distance);
                buf.writeVarInt(p.mode);
                buf.writeVarInt(p.variant);
                buf.writeBoolean(p.mobility);
            },
            buf -> new UnseenHandSyncPayload(UUIDUtil.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readBoolean(),
                    buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    @Override
    public Type<UnseenHandSyncPayload> type() {
        return TYPE;
    }
}
