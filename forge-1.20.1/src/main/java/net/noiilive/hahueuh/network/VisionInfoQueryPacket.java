package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class VisionInfoQueryPacket {
    public final String query;

    public VisionInfoQueryPacket(String query) {
        this.query = query;
    }

    public VisionInfoQueryPacket(FriendlyByteBuf buf) {
        this.query = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(query, 256);
    }

    public static void handle(VisionInfoQueryPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.VISION_OF_INFORMATION.handleQuery(sender, packet.query);
        });
        ctx.setPacketHandled(true);
    }
}
