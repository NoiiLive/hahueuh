package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class SecondShiftTogglePacket {
    public static final SecondShiftTogglePacket INSTANCE = new SecondShiftTogglePacket();

    public SecondShiftTogglePacket() {}

    public SecondShiftTogglePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(SecondShiftTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.SECOND_SHIFT.toggle(sender);
        });
        ctx.setPacketHandled(true);
    }
}
