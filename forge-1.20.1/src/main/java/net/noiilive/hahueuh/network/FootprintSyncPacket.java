package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FootprintSyncPacket {
    public record Footprint(double x, double y, double z, float yaw, int category, long timestamp, String name) {}

    public final int maxAgeTicks;
    public final List<Footprint> footprints;

    public FootprintSyncPacket(int maxAgeTicks, List<Footprint> footprints) {
        this.maxAgeTicks = maxAgeTicks;
        this.footprints = footprints;
    }

    public int maxAgeTicks() { return maxAgeTicks; }

    public List<Footprint> footprints() { return footprints; }

    public FootprintSyncPacket(FriendlyByteBuf buf) {
        this.maxAgeTicks = buf.readVarInt();
        int count = buf.readVarInt();
        List<Footprint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Footprint(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readVarInt(), buf.readLong(), buf.readUtf()));
        }
        this.footprints = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maxAgeTicks);
        buf.writeVarInt(footprints.size());
        for (Footprint f : footprints) {
            buf.writeDouble(f.x());
            buf.writeDouble(f.y());
            buf.writeDouble(f.z());
            buf.writeFloat(f.yaw());
            buf.writeVarInt(f.category());
            buf.writeLong(f.timestamp());
            buf.writeUtf(f.name());
        }
    }

    public static void handle(FootprintSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientFootprintState.set(packet.maxAgeTicks, packet.footprints)));
        ctx.setPacketHandled(true);
    }
}
