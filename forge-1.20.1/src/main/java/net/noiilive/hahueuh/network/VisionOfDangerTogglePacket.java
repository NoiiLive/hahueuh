package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class VisionOfDangerTogglePacket {
    public static final VisionOfDangerTogglePacket INSTANCE = new VisionOfDangerTogglePacket();

    public VisionOfDangerTogglePacket() {}

    public VisionOfDangerTogglePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(VisionOfDangerTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.VISION_OF_DANGER.toggle(sender);
        });
        ctx.setPacketHandled(true);
    }
}
