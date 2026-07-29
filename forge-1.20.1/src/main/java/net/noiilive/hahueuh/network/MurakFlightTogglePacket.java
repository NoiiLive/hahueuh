package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class MurakFlightTogglePacket {
    private final boolean wantsFlight;

    public MurakFlightTogglePacket(boolean wantsFlight) {
        this.wantsFlight = wantsFlight;
    }

    public MurakFlightTogglePacket(FriendlyByteBuf buf) {
        this.wantsFlight = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(wantsFlight);
    }

    public static void handle(MurakFlightTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            HahUeuh.MURAK.setFlying(player, packet.wantsFlight);
        });
        ctx.setPacketHandled(true);
    }
}
