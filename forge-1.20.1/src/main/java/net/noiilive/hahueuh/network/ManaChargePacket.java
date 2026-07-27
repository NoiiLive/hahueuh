package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class ManaChargePacket {
    private final boolean charging;

    public ManaChargePacket(boolean charging) {
        this.charging = charging;
    }

    public ManaChargePacket(FriendlyByteBuf buf) {
        this.charging = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(charging);
    }

    public static void handle(ManaChargePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                HahUeuh.MANA_CHARGING.setCharging(sender, packet.charging);
            }
        });
        ctx.setPacketHandled(true);
    }
}
