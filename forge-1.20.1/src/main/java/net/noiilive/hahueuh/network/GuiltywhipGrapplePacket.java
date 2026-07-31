package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class GuiltywhipGrapplePacket {
    public final int targetId;
    public final boolean hasBlock;
    public final double x;
    public final double y;
    public final double z;

    public GuiltywhipGrapplePacket(int targetId, boolean hasBlock, double x, double y, double z) {
        this.targetId = targetId;
        this.hasBlock = hasBlock;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GuiltywhipGrapplePacket(FriendlyByteBuf buf) {
        this.targetId = buf.readVarInt();
        this.hasBlock = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetId);
        buf.writeBoolean(hasBlock);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static void handle(GuiltywhipGrapplePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                HahUeuh.GUILTYWHIP.handleGrapple(player, packet.targetId,
                        packet.hasBlock ? new Vec3(packet.x, packet.y, packet.z) : null);
            }
        });
        ctx.setPacketHandled(true);
    }
}
