package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class AbilitySlotsUpdatePacket {
    private final AbilitySlotsData data;

    public AbilitySlotsUpdatePacket(AbilitySlotsData data) {
        this.data = data;
    }

    public AbilitySlotsUpdatePacket(FriendlyByteBuf buf) {
        this.data = AbilitySlotsData.decode(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        data.encode(buf);
    }

    public static void handle(AbilitySlotsUpdatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                HahUeuh.SNAPSHOT_MANAGER.getAbilitySlotsManager().update(sender.getUUID(), packet.data);
            }
        });
        ctx.setPacketHandled(true);
    }
}
