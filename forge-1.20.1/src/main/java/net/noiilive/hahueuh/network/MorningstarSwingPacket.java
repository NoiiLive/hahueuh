package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class MorningstarSwingPacket {
    public final boolean spin;

    public MorningstarSwingPacket(boolean spin) {
        this.spin = spin;
    }

    public MorningstarSwingPacket(FriendlyByteBuf buf) {
        this.spin = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(spin);
    }

    public static void handle(MorningstarSwingPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.MORNINGSTAR.handleSwing(sender, packet.spin);
        });
        ctx.setPacketHandled(true);
    }
}
