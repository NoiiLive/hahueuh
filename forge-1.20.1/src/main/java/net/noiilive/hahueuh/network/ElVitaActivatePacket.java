package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class ElVitaActivatePacket {
    public static final ElVitaActivatePacket INSTANCE = new ElVitaActivatePacket();

    public ElVitaActivatePacket() {}

    public ElVitaActivatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ElVitaActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            HahUeuh.EL_VITA.tryCast(player);
        });
        ctx.setPacketHandled(true);
    }
}
