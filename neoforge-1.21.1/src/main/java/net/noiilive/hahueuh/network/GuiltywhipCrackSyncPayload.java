package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GuiltywhipCrackSyncPayload(UUID owner, boolean sweep) implements CustomPacketPayload {
    public static final Type<GuiltywhipCrackSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "guiltywhip_crack_sync"));

    public static final StreamCodec<ByteBuf, GuiltywhipCrackSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, GuiltywhipCrackSyncPayload::owner,
                    ByteBufCodecs.BOOL, GuiltywhipCrackSyncPayload::sweep,
                    GuiltywhipCrackSyncPayload::new);

    @Override
    public Type<GuiltywhipCrackSyncPayload> type() {
        return TYPE;
    }
}
