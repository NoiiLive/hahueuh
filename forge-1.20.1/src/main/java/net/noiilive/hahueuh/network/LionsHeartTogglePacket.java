package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class LionsHeartTogglePacket {
    public static final LionsHeartTogglePacket INSTANCE = new LionsHeartTogglePacket();

    public LionsHeartTogglePacket() {}

    public LionsHeartTogglePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(LionsHeartTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.LIONS_HEART.toggle(sender);
        });
        ctx.setPacketHandled(true);
    }
}
