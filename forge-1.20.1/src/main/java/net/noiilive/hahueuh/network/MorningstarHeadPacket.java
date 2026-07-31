package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MorningstarHeadPacket {
    public final UUID owner;
    public final double x;
    public final double y;
    public final double z;
    public final double vx;
    public final double vy;
    public final double vz;

    public MorningstarHeadPacket(UUID owner, double x, double y, double z,
                                 double vx, double vy, double vz) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
    }

    public MorningstarHeadPacket(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.vx = buf.readDouble();
        this.vy = buf.readDouble();
        this.vz = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(vx);
        buf.writeDouble(vy);
        buf.writeDouble(vz);
    }

    public static void handle(MorningstarHeadPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.noiilive.hahueuh.client.MorningstarClient.applyHeadSync(
                        packet.owner,
                        new Vec3(packet.x, packet.y, packet.z),
                        new Vec3(packet.vx, packet.vy, packet.vz))));
        ctx.setPacketHandled(true);
    }
}
