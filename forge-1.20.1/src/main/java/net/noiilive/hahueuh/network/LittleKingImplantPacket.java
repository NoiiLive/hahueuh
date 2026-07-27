package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class LittleKingImplantPacket {
    public static final LittleKingImplantPacket INSTANCE = new LittleKingImplantPacket();

    public LittleKingImplantPacket() {}

    public LittleKingImplantPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(LittleKingImplantPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.LITTLE_KING.implant(sender);
        });
        ctx.setPacketHandled(true);
    }
}
