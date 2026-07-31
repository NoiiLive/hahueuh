package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class GuiltywhipCrackSyncPacket {
    public final UUID owner;
    public final boolean sweep;

    public GuiltywhipCrackSyncPacket(UUID owner, boolean sweep) {
        this.owner = owner;
        this.sweep = sweep;
    }

    public GuiltywhipCrackSyncPacket(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.sweep = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeBoolean(sweep);
    }

    public static void handle(GuiltywhipCrackSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.noiilive.hahueuh.client.GuiltywhipClient.applyRemoteCrack(packet.owner, packet.sweep)));
        ctx.setPacketHandled(true);
    }
}
