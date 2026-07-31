package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class GuiltywhipCrackPacket {
    public final boolean sweep;

    public GuiltywhipCrackPacket(boolean sweep) {
        this.sweep = sweep;
    }

    public GuiltywhipCrackPacket(FriendlyByteBuf buf) {
        this.sweep = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(sweep);
    }

    public static void handle(GuiltywhipCrackPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.GUILTYWHIP.handleCrack(sender, packet.sweep);
        });
        ctx.setPacketHandled(true);
    }
}
