package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MorningstarSwingSyncPacket {
    public final UUID owner;
    public final boolean spin;

    public MorningstarSwingSyncPacket(UUID owner, boolean spin) {
        this.owner = owner;
        this.spin = spin;
    }

    public MorningstarSwingSyncPacket(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.spin = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeBoolean(spin);
    }

    public static void handle(MorningstarSwingSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.noiilive.hahueuh.client.MorningstarClient.applyRemoteSwing(packet.owner, packet.spin)));
        ctx.setPacketHandled(true);
    }
}
