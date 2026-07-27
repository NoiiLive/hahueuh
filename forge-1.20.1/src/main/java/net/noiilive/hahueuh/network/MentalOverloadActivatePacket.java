package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class MentalOverloadActivatePacket {
    public static final MentalOverloadActivatePacket INSTANCE = new MentalOverloadActivatePacket();

    public MentalOverloadActivatePacket() {}

    public MentalOverloadActivatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(MentalOverloadActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.MENTAL_OVERLOAD.activate(sender);
        });
        ctx.setPacketHandled(true);
    }
}
