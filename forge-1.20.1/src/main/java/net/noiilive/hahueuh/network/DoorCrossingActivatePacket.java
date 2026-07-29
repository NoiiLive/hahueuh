package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class DoorCrossingActivatePacket {
    public static final DoorCrossingActivatePacket INSTANCE = new DoorCrossingActivatePacket();

    public DoorCrossingActivatePacket() {}

    public DoorCrossingActivatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(DoorCrossingActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            HahUeuh.DOOR_CROSSING.tryCast(player);
        });
        ctx.setPacketHandled(true);
    }
}
