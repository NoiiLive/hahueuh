package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.Miasma;

import java.util.function.Supplier;

public class AllyTrackerActivatePacket {
    public static final AllyTrackerActivatePacket INSTANCE = new AllyTrackerActivatePacket();

    public AllyTrackerActivatePacket() {}

    public AllyTrackerActivatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(AllyTrackerActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            HahUeuh.ALLY_TRACKER.activate(sender);
            Miasma.addSingleUse(sender);
        });
        ctx.setPacketHandled(true);
    }
}
