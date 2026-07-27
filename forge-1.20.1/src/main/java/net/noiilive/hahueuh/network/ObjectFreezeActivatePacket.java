package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class ObjectFreezeActivatePacket {
    public static final ObjectFreezeActivatePacket INSTANCE = new ObjectFreezeActivatePacket();

    public ObjectFreezeActivatePacket() {}

    public ObjectFreezeActivatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ObjectFreezeActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.OBJECT_FREEZE.activate(sender);
        });
        ctx.setPacketHandled(true);
    }
}
