package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class BackToEnchantingPacket {
    public static final BackToEnchantingPacket INSTANCE = new BackToEnchantingPacket();

    public BackToEnchantingPacket() {}

    public BackToEnchantingPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(BackToEnchantingPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.EFFICIENT_ENCHANTING.goBack(sender);
        });
        ctx.setPacketHandled(true);
    }
}
