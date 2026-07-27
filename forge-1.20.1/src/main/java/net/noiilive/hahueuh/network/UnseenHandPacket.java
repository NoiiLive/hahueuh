package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class UnseenHandPacket {
    private final boolean active;
    private final float distance;
    private final int mode;
    private final boolean mobility;
    private final boolean quickSession;

    public UnseenHandPacket(boolean active, float distance, int mode, boolean mobility, boolean quickSession) {
        this.active = active;
        this.distance = distance;
        this.mode = mode;
        this.mobility = mobility;
        this.quickSession = quickSession;
    }

    public UnseenHandPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.distance = buf.readFloat();
        this.mode = buf.readVarInt();
        this.mobility = buf.readBoolean();
        this.quickSession = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeFloat(distance);
        buf.writeVarInt(mode);
        buf.writeBoolean(mobility);
        buf.writeBoolean(quickSession);
    }

    public static void handle(UnseenHandPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                HahUeuh.SNAPSHOT_MANAGER.onUnseenHandUpdate(sender, packet.active, packet.distance,
                        packet.mode, packet.mobility, packet.quickSession);
            }
        });
        ctx.setPacketHandled(true);
    }
}
