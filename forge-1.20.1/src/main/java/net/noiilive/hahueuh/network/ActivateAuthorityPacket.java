package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class ActivateAuthorityPacket {
    private final boolean aggressor;

    public ActivateAuthorityPacket(boolean aggressor) {
        this.aggressor = aggressor;
    }

    public ActivateAuthorityPacket(FriendlyByteBuf buf) {
        this.aggressor = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(aggressor);
    }

    public static void handle(ActivateAuthorityPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                HahUeuh.SNAPSHOT_MANAGER.toggleDomain(sender, packet.aggressor);
            }
        });
        ctx.setPacketHandled(true);
    }
}
