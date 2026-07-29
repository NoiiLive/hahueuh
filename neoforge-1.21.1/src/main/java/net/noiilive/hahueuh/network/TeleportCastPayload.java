package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportCastPayload(int x, int y, int z, boolean portal) implements CustomPacketPayload {
    public static final Type<TeleportCastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "teleport_cast"));

    public static final StreamCodec<FriendlyByteBuf, TeleportCastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeInt(p.x);
                buf.writeInt(p.y);
                buf.writeInt(p.z);
                buf.writeBoolean(p.portal);
            },
            buf -> new TeleportCastPayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean()));

    @Override
    public Type<TeleportCastPayload> type() {
        return TYPE;
    }
}
