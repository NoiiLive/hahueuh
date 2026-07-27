package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class BaseShiftTogglePacket {
    public static final BaseShiftTogglePacket INSTANCE = new BaseShiftTogglePacket();

    public BaseShiftTogglePacket() {}

    public BaseShiftTogglePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(BaseShiftTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.BASE_SHIFT.toggle(sender);
        });
        ctx.setPacketHandled(true);
    }
}
