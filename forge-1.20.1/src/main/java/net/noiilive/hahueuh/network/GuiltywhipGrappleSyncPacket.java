package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class GuiltywhipGrappleSyncPacket {
    public final UUID owner;
    public final int targetId;
    public final boolean hasBlock;
    public final double x;
    public final double y;
    public final double z;

    public GuiltywhipGrappleSyncPacket(UUID owner, int targetId, boolean hasBlock,
                                       double x, double y, double z) {
        this.owner = owner;
        this.targetId = targetId;
        this.hasBlock = hasBlock;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GuiltywhipGrappleSyncPacket(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.targetId = buf.readVarInt();
        this.hasBlock = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeVarInt(targetId);
        buf.writeBoolean(hasBlock);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static void handle(GuiltywhipGrappleSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.noiilive.hahueuh.client.GuiltywhipClient.applyRemoteGrapple(
                        packet.owner, packet.targetId,
                        packet.hasBlock ? new Vec3(packet.x, packet.y, packet.z) : null)));
        ctx.setPacketHandled(true);
    }
}
