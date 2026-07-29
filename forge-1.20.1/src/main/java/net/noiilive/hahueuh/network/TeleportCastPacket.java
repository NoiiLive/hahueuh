package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class TeleportCastPacket {
    private final int x;
    private final int y;
    private final int z;
    private final boolean portal;

    public TeleportCastPacket(int x, int y, int z, boolean portal) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.portal = portal;
    }

    public TeleportCastPacket(FriendlyByteBuf buf) {
        this.x = buf.readVarInt();
        this.y = buf.readVarInt();
        this.z = buf.readVarInt();
        this.portal = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(z);
        buf.writeBoolean(portal);
    }

    public static void handle(TeleportCastPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            HahUeuh.TELEPORTATION.request(player, packet.x, packet.y, packet.z, packet.portal);
        });
        ctx.setPacketHandled(true);
    }
}
