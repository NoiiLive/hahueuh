package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record FootprintSyncPayload(int maxAgeTicks, List<Footprint> footprints) implements CustomPacketPayload {
    public static final Type<FootprintSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "footprint_sync"));

    public record Footprint(double x, double y, double z, float yaw, int category, long timestamp, String name) {}

    public static final StreamCodec<ByteBuf, FootprintSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf b = new FriendlyByteBuf(buf);
                b.writeVarInt(payload.maxAgeTicks());
                b.writeVarInt(payload.footprints().size());
                for (Footprint f : payload.footprints()) {
                    b.writeDouble(f.x());
                    b.writeDouble(f.y());
                    b.writeDouble(f.z());
                    b.writeFloat(f.yaw());
                    b.writeByte(f.category());
                    b.writeLong(f.timestamp());
                    b.writeUtf(f.name(), 64);
                }
            },
            buf -> {
                FriendlyByteBuf b = new FriendlyByteBuf(buf);
                int maxAge = b.readVarInt();
                int count = b.readVarInt();
                List<Footprint> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    double x = b.readDouble();
                    double y = b.readDouble();
                    double z = b.readDouble();
                    float yaw = b.readFloat();
                    int category = b.readByte();
                    long timestamp = b.readLong();
                    String name = b.readUtf(64);
                    list.add(new Footprint(x, y, z, yaw, category, timestamp, name));
                }
                return new FootprintSyncPayload(maxAge, list);
            });

    @Override
    public Type<FootprintSyncPayload> type() {
        return TYPE;
    }
}
